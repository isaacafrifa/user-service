# Book Me - User Service 
BookMe manages bookings and this repository is the user service for BookMe. The service provides comprehensive user management capabilities including user creation, retrieval, updating, deletion, and advanced search functionality.

## Functionalities

### User Management
- Create Users, Retrieve Users, Update Users, Delete Users

### Advanced Search Capabilities
- **Filter-based Search**: Search users by various criteria:
  - User IDs
  - First names
  - Last names
  - Email addresses
  - Phone numbers
- **Text Search**: Free-form text search across multiple user fields

### Event Publishing
- **Email Update Events**: Publish events when a user's email is updated, allowing other services to react accordingly

### Caching
- **User Data Caching**: Improve performance with Hazelcast-based caching

### Security
- **JWT Authentication**: Secure all API endpoints with JWT-based authentication

## API Endpoints
- Check SwaggerUI

## Technologies 
- Java Spring Boot
- Test containers
- Postgres
- Docker Compose
- Cloud-native buildpacks instead of Dockerfile
- Hazelcast for caching
- Kubernetes
- RabbitMQ for event production
- Flyway
- Spring Doc OpenAPI Specification
- Loki
- Micrometer Tracing for distributed tracing
- Prometheus
- Maven

## Application Architecture 
*This diagram was automatically created by* [GitDiagram](https://gitdiagram.com)

![diagram (2)](https://github.com/user-attachments/assets/f2d4efe4-ea36-4479-a589-4f2fcf113fe6)


_Stay tuned for further updates!_
