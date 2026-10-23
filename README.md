# NexusMarket

Marketplace Spring Boot con catálogo, usuarios, carrito, pedidos, inventario, envíos y facturación.

## Ejecutar

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

La tienda queda en `http://localhost:8080`. Los datos de desarrollo se guardan en `data/nexusmarket.mv.db`; ese directorio está excluido de Git.

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
- `GET /api/customers/{id}/cart`, `POST /api/customers/{id}/checkout`
- `POST /api/orders/{code}/payment`
- `POST /api/warehouses`, `POST /api/inventory/stock`
- `POST /api/shipments`, `POST /api/shipments/{code}/dispatch`, `POST /api/shipments/{code}/deliver`
- `POST /api/invoices`, `GET /api/invoices/{number}/export`
- `GET /api/reports/{sales|inventory|shipments}`

## Límites del entorno local

- Los repositorios persisten objetos del dominio serializados en H2. Es almacenamiento local duradero para desarrollo, no un esquema relacional listo para producción.
- El gateway de pagos del perfil `dev` es un sandbox determinista; no procesa cargos reales. Para producción se debe implementar `PaymentGatewayPort` con un proveedor elegido y credenciales en un gestor de secretos.
- Notificaciones y seguimiento usan adaptadores locales. No envían correo ni consultan transportistas externos.
- La API usa HTTP Basic. En un despliegue real debe servirse sobre TLS y revisarse la política de autenticación y sesiones.
- La tasa tributaria local se configura con `NEXUSMARKET_TAX_RATE`; confirma la tasa aplicable antes de usar facturación real.
