
techology stack:
* Spring Boot 4.0.2
* Spring Security
* Spring Data JPA
* Spring Cloud Gateway
* Gradle 9.2

## Environment Variables
* You can use the KeyGen.main.kts kotlin script to generate a pair of RSA keys.
``` toml
# Main Backend
SALT_EDGE_API_SECRET
SALT_EDGE_APP_ID=

MAIN_DB_NAME=main_db
MAIN_DB_HOST=os-main-backend-database:5432
MAIN_DB_USER=admin
MAIN_DB_PASSWORD=admin

# Gateway
RESOURCE_SERVER_BASE_URL=http://os-authenticator:8081

# Authenticator
AUTH_DB_NAME=auth_db
AUTH_DB_HOST=auth_db:5432
AUTH_DB_USER=admin
AUTH_DB_PASSWORD=admin
OMNISENTRY_RSA_PUBLIC_KEY=your_public_key
OMNISENTRY_RSA_PRIVATE_KEY=your_private_key
```
