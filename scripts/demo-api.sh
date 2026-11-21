#!/bin/bash
# Guion de demo por API: ejecuta los flujos 1-6 con validaciones GET y pruebas negativas por rol.
# Uso (BD vacía, desde una carpeta de trabajo): NEXUS_ADMIN=admin:<clave> bash scripts/demo-api.sh 2>&1 >/dev/null
B=${B:-localhost:8080/api}
H='Content-Type: application/json'
ADM="${NEXUS_ADMIN:?Define NEXUS_ADMIN=usuario:contraseña del administrador}"
r() { # r LABEL USER:PASS METHOD PATH [BODY]
  local label=$1 auth=$2 m=$3 p=$4 body=$5 a=()
  [ "$auth" != "-" ] && a=(-u "$auth")
  if [ -n "$body" ]; then out=$(curl -s -w '\n%{http_code}' "${a[@]}" -X $m -H "$H" -d "$body" "$B$p")
  else out=$(curl -s -w '\n%{http_code}' "${a[@]}" -X $m "$B$p"); fi
  code=${out##*$'\n'}; body_out=${out%$'\n'*}
  echo "[$code] $label :: ${body_out:0:400}" >&2
  echo "$body_out"
}
jq_() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1"; }

echo "== 1. Vendedores, compradores y productos ==" >&2
for i in 1 2 3; do
  r "registro vendedor $i" - POST /auth/register/seller "{\"fullName\":\"Vend $i\",\"email\":\"v$i@t.test\",\"phoneNumber\":\"300000000$i\",\"businessName\":\"Tienda$i\",\"taxIdentificationNumber\":90000000$i,\"username\":\"vend$i\",\"password\":\"VendTest12345\",\"address\":\"Calle $i\"}" > s$i.json
  r "registro comprador $i" - POST /auth/register/customer "{\"fullName\":\"Comp $i\",\"email\":\"c$i@t.test\",\"phoneNumber\":\"310000000$i\",\"address\":\"Carrera $i\",\"username\":\"comp$i\",\"password\":\"CompTest12345\"}" > c$i.json
done
for i in 1 2 3; do r "GET /auth/me vend$i" vend$i:VendTest12345 GET /auth/me >/dev/null; r "GET /auth/me comp$i" comp$i:CompTest12345 GET /auth/me >/dev/null; done
for i in 1 2 3; do
  SC=$(jq_ "d['sellerCode']" < s$i.json); echo "$SC" > sc$i
  r "vend$i crea fisico" vend$i:VendTest12345 POST /products "{\"name\":\"Fisico$i\",\"description\":\"f\",\"price\":$((i*10)).0,\"type\":\"PHYSICAL\",\"sellerCode\":\"$SC\"}" | jq_ "d['productCode']" > pf$i
  r "vend$i crea digital" vend$i:VendTest12345 POST /products "{\"name\":\"Digital$i\",\"description\":\"d\",\"price\":$((i*5)).0,\"type\":\"DIGITAL\",\"sellerCode\":\"$SC\"}" | jq_ "d['productCode']" > pd$i
  r "GET productos del vendedor (incluye no publicados)" vend$i:VendTest12345 GET /sellers/$SC/products >/dev/null
  r "vend$i publica fisico" vend$i:VendTest12345 POST /products/$(cat pf$i)/publish >/dev/null
  r "vend$i publica digital" vend$i:VendTest12345 POST /products/$(cat pd$i)/publish >/dev/null
  r "GET producto fisico publicado" - GET /products/$(cat pf$i) >/dev/null
done
echo "== Seguridad productos ==" >&2
r "NEG vend2 publica producto de vend1" vend2:VendTest12345 POST /products/$(cat pf1)/publish >/dev/null
r "NEG vend2 crea producto a nombre de vend1" vend2:VendTest12345 POST /products "{\"name\":\"X\",\"description\":\"x\",\"price\":1.0,\"type\":\"DIGITAL\",\"sellerCode\":\"$(cat sc1)\"}" >/dev/null
r "NEG comp1 crea producto" comp1:CompTest12345 POST /products "{\"name\":\"X\",\"description\":\"x\",\"price\":1.0,\"type\":\"DIGITAL\",\"sellerCode\":\"$(cat sc1)\"}" >/dev/null
r "NEG anonimo crea producto" - POST /products '{}' >/dev/null

echo "== Inventario ==" >&2
r "admin crea bodega" $ADM POST /warehouses '{"warehouseCode":"WH1","name":"Bodega","address":"Zona","capacity":1000}' >/dev/null
r "NEG vend1 crea bodega" vend1:VendTest12345 POST /warehouses '{"warehouseCode":"WH2","name":"B","address":"Z","capacity":10}' >/dev/null
r "NEG comp1 agrega stock" comp1:CompTest12345 POST /inventory/stock "{\"productCode\":\"$(cat pf1)\",\"warehouseCode\":\"WH1\",\"quantity\":5}" >/dev/null
for i in 1 2 3; do r "admin stock fisico$i=5" $ADM POST /inventory/stock "{\"productCode\":\"$(cat pf$i)\",\"warehouseCode\":\"WH1\",\"quantity\":5}" >/dev/null; done
for i in 1 2 3; do r "GET inventario fisico$i" $ADM GET /inventory/$(cat pf$i) >/dev/null; done
r "NEG vend1 consulta inventario admin" vend1:VendTest12345 GET /inventory/$(cat pf1) >/dev/null

echo "== 2. Carrito comp1 ==" >&2
C1=$(jq_ "d['personId']" < c1.json); C2=$(jq_ "d['personId']" < c2.json)
for p in $(cat pf1) $(cat pf2) $(cat pd1) $(cat pd2); do r "comp1 agrega $p x1" comp1:CompTest12345 POST /customers/$C1/cart/items "{\"productCode\":\"$p\",\"quantity\":1}" >/dev/null; done
r "GET carrito" comp1:CompTest12345 GET /customers/$C1/cart >/dev/null
r "OVERSTOCK comp1 agrega fisico3 x99 (stock 5)" comp1:CompTest12345 POST /customers/$C1/cart/items "{\"productCode\":\"$(cat pf3)\",\"quantity\":99}" >/dev/null
r "GET carrito tras overstock" comp1:CompTest12345 GET /customers/$C1/cart >/dev/null
r "comp1 agrega fisico3 x1 y lo quita (DELETE)" comp1:CompTest12345 POST /customers/$C1/cart/items "{\"productCode\":\"$(cat pf3)\",\"quantity\":1}" >/dev/null
r "comp1 quita fisico3" comp1:CompTest12345 DELETE /customers/$C1/cart/items/$(cat pf3) >/dev/null
r "GET carrito final (4 items)" comp1:CompTest12345 GET /customers/$C1/cart | python3 -c "import sys,json;print('CARRITO:',[(i['name'],i['quantity']) for i in json.load(sys.stdin)['items']])" >&2
r "NEG comp2 lee carrito de comp1" comp2:CompTest12345 GET /customers/$C1/cart >/dev/null
r "NEG vend1 lee carrito de comp1" vend1:VendTest12345 GET /customers/$C1/cart >/dev/null
r "NEG comp2 agrega al carrito de comp1" comp2:CompTest12345 POST /customers/$C1/cart/items "{\"productCode\":\"$(cat pd3)\",\"quantity\":1}" >/dev/null

echo "== 3. Compra y envio ==" >&2
OC=$(r "comp1 checkout" comp1:CompTest12345 POST /customers/$C1/checkout | jq_ "d['orderCode']"); echo $OC > oc
r "GET pedido" comp1:CompTest12345 GET /orders/$OC >/dev/null
r "NEG comp2 paga pedido de comp1" comp2:CompTest12345 POST /orders/$OC/payment >/dev/null
r "comp1 paga" comp1:CompTest12345 POST /orders/$OC/payment >/dev/null
r "GET pedido tras pago" comp1:CompTest12345 GET /orders/$OC >/dev/null
r "NEG comp1 crea envio" comp1:CompTest12345 POST /shipments "{\"orderCode\":\"$OC\",\"warehouseCode\":\"WH1\"}" >/dev/null
SH=$(r "admin crea envio" $ADM POST /shipments "{\"orderCode\":\"$OC\",\"warehouseCode\":\"WH1\"}" | jq_ "d['shipmentCode']"); echo $SH > sh
r "admin despacha" $ADM POST /shipments/$SH/dispatch >/dev/null
r "admin entrega" $ADM POST /shipments/$SH/deliver >/dev/null
r "GET envio (comp1)" comp1:CompTest12345 GET /shipments/$SH >/dev/null
r "NEG comp2 lee envio" comp2:CompTest12345 GET /shipments/$SH >/dev/null
r "GET pedido final" comp1:CompTest12345 GET /orders/$OC >/dev/null
r "GET disponibilidad fisico1 tras envio" - GET /products/$(cat pf1)/availability >/dev/null
INV=$(r "comp1 genera factura" comp1:CompTest12345 POST /invoices "{\"orderCode\":\"$OC\"}" | jq_ "d.get('invoiceNumber')")
r "GET factura" comp1:CompTest12345 GET /invoices/$INV >/dev/null
r "NEG comp2 lee factura" comp2:CompTest12345 GET /invoices/$INV >/dev/null

echo "== 4. Devolucion / reembolso ==" >&2
r "NEG comp2 devuelve producto de comp1" comp2:CompTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pf2)\",\"quantity\":1}" >/dev/null
r "NEG vend2 devuelve" vend2:VendTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pf2)\",\"quantity\":1}" >/dev/null
r "NEG comp1 devuelve digital" comp1:CompTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pd2)\",\"quantity\":1}" >/dev/null
r "NEG comp1 devuelve 2 unidades (compro 1)" comp1:CompTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pf2)\",\"quantity\":2}" >/dev/null
r "comp1 devuelve fisico2 (vend2)" comp1:CompTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pf2)\",\"quantity\":1}" >/dev/null
r "GET devoluciones del pedido" comp1:CompTest12345 GET /orders/$OC/returns >/dev/null
r "GET pedido tras devolucion" comp1:CompTest12345 GET /orders/$OC | python3 -c "import sys,json;d=json.load(sys.stdin);print('PEDIDO',d['status'],'reembolsado',d['refundedAmount']);[print('  ',l['name'],l['sellerCode'][:12],l['quantity'],l['returnedQuantity'],l['status']) for l in d['lines']]" >&2
r "NEG comp1 repite devolucion fisico2" comp1:CompTest12345 POST /orders/$OC/returns "{\"productCode\":\"$(cat pf2)\",\"quantity\":1}" >/dev/null
r "GET inventario fisico2 tras devolucion" $ADM GET /inventory/$(cat pf2) >/dev/null

echo "== 5. Reportes ==" >&2
for i in 1 2 3; do r "vend$i report" vend$i:VendTest12345 GET /sellers/$(cat sc$i)/report > rep_v$i.json; done
r "NEG vend2 report de vend1" vend2:VendTest12345 GET /sellers/$(cat sc1)/report >/dev/null
r "NEG comp1 report de vend1" comp1:CompTest12345 GET /sellers/$(cat sc1)/report >/dev/null
r "NEG vend1 reporte global" vend1:VendTest12345 GET /reports/sales >/dev/null
r "NEG comp1 admin orders" comp1:CompTest12345 GET /admin/orders >/dev/null
r "admin orders" $ADM GET /admin/orders >/dev/null
for t in sales inventory shipments; do r "admin reporte $t" $ADM GET /reports/$t > rep_$t.csv; done
