# NexusMarket

Marketplace Spring Boot con catálogo, usuarios, carrito, pedidos, inventario, envíos y facturación.

## Ejecutar

PowerShell:

```powershell
.\run.cmd
```

La tienda queda en `http://localhost:8080`. Los datos de desarrollo se guardan en `data/nexusmarket.mv.db`; ese directorio está excluido de Git.

## Publicar en Render

El proyecto incluye un `Dockerfile` y un Blueprint en `render.yaml`. Para publicarlo:

1. Sube este proyecto a un repositorio de GitHub.
2. En Render, crea un **Blueprint** y conecta el repositorio.
3. Configura `NEXUSMARKET_BOOTSTRAP_ADMIN_USERNAME` y `NEXUSMARKET_BOOTSTRAP_ADMIN_PASSWORD` cuando Render los solicite. La contraseña debe tener al menos 12 caracteres.
4. Revisa el plan y el disco persistente indicados por el Blueprint antes de confirmar la creación. Render requiere un servicio de pago para conservar datos en un disco persistente.
5. Cuando el despliegue termine, Render mostrará la URL pública `https://...onrender.com` para compartir.

El servicio usa el perfil `dev`, por lo que los pagos son de prueba. H2 guarda sus datos en el disco persistente montado en `/var/data`.

Las pruebas usan una base H2 en memoria:

```powershell
.\mvnw.cmd test
```

## Administrador inicial

El registro público solo crea clientes y vendedores. Para crear el primer administrador, establece estas variables antes de arrancar. La contraseña debe tener al menos 12 caracteres y se almacena con BCrypt.

```powershell
$env:NEXUSMARKET_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:NEXUSMARKET_BOOTSTRAP_ADMIN_PASSWORD = "<contraseña de al menos 12 caracteres>"
.\mvnw.cmd spring-boot:run
```

El bootstrap no reemplaza ni modifica un administrador existente. No guardes las credenciales en el repositorio.

## API

La API usa HTTP Basic para operaciones protegidas. El registro y login, el catálogo publicado y la página de inicio son públicos. El resto de operaciones requiere autenticación y autorización por rol, además de validaciones de propiedad para carritos, pedidos, facturas y productos de vendedores.

Rutas principales:

- `POST /api/auth/register/customer` y `POST /api/auth/register/seller`
- `POST /api/auth/login`
- `GET /api/products`, `POST /api/products`, `POST /api/products/{code}/publish`
- `GET /api/auth/me` (usuario autenticado)
- `GET /api/sellers/{code}/products` (productos del vendedor, incluso sin publicar)
- `GET /api/customers/{id}/cart`, `POST /api/customers/{id}/cart/items` (valida stock físico), `POST /api/customers/{id}/checkout`
- `GET /api/orders/{code}`, `POST /api/orders/{code}/payment`
- `POST /api/orders/{code}/returns` y `GET /api/orders/{code}/returns`: devolución y reembolso de un producto físico entregado; identifica al vendedor, repone stock en la bodega de origen y no modifica los demás ítems
- `GET /api/inventory/{productCode}` (administrador/logística)
- `POST /api/warehouses`, `POST /api/inventory/stock`
- `POST /api/shipments`, `POST /api/shipments/{code}/dispatch`, `POST /api/shipments/{code}/deliver`
- `POST /api/invoices`, `GET /api/invoices/{number}/export`
- `GET /api/sellers/{code}/report`: ventas, inventario y movimientos (compras, devoluciones, reembolsos) del vendedor
- `GET /api/admin/orders`, `GET /api/admin/sellers` (administrador/supervisor)
- `GET /api/reports/{sales|inventory|shipments}`

`scripts/demo-api.sh` recorre todos los flujos con sus GET de verificación y los intentos bloqueados por rol.

## Límites del entorno local

- Los repositorios persisten objetos del dominio serializados en H2. Es almacenamiento local duradero para desarrollo, no un esquema relacional listo para producción.
- El gateway de pagos del perfil `dev` es un sandbox determinista; no procesa cargos reales. Para producción se debe implementar `PaymentGatewayPort` con un proveedor elegido y credenciales en un gestor de secretos.
- Notificaciones y seguimiento usan adaptadores locales. No envían correo ni consultan transportistas externos.
- La API usa HTTP Basic. En un despliegue real debe servirse sobre TLS y revisarse la política de autenticación y sesiones.
- La tasa tributaria local se configura con `NEXUSMARKET_TAX_RATE`; confirma la tasa aplicable antes de usar facturación real.
