# Layer Dog - Architecture Enforcer Plugin

An IntelliJ IDEA plugin that enforces architectural layering rules in Java projects using Maven build system.

## Architecture Layers

The plugin enforces the following layering rules:

### 1. Controller Layer
- **Purpose**: Handle HTTP requests/responses
- **Rules**: 
  - Should have no business logic
  - Should only call DTO layer
- **Naming conventions**: Classes ending with `Controller`, `Resource`, `Endpoint` or in packages containing `controller`, `web`, `rest`

### 2. DTO Layer (Data Transfer Object)
- **Purpose**: Data validation, conversion, and transfer
- **Rules**:
  - Should only call API layer
  - Should do validations and conversions
  - Should have no business logic
- **Naming conventions**: Classes ending with `DTO`, `Dto`, `Request`, `Response`, `Model` or in packages containing `dto`, `model`

### 3. API Layer (Service/Business Layer)
- **Purpose**: Contains all business logic
- **Rules**:
  - Should contain business logic
  - Should call DAO or FLOW layers
  - Should not call other API layers directly
- **Naming conventions**: Classes ending with `Service`, `ServiceImpl`, `Manager`, `Handler`, `Processor` or in packages containing `service`, `business`, `logic`

### 4. FLOW Layer
- **Purpose**: Orchestrate multiple API calls
- **Rules**:
  - Should be used when API needs to call other APIs
  - Should orchestrate API operations
  - Should call API layers only
- **Naming conventions**: Classes ending with `Flow`, `Workflow`, `Orchestrator`, `Coordinator` or in packages containing `flow`, `workflow`, `orchestrat`

### 5. DAO Layer (Data Access Object)
- **Purpose**: Database interaction
- **Rules**:
  - Should only talk to database
  - Should contain no business logic
  - Should handle data persistence operations
- **Naming conventions**: Classes ending with `DAO`, `Dao`, `Repository`, `Mapper`, `Entity` or in packages containing `dao`, `repository`, `data`, `persistence`

## Installation

### Building the Plugin

1. Clone this repository
2. Open in IntelliJ IDEA
3. Run the Gradle task: `./gradlew buildPlugin`
4. The plugin will be built in `build/distributions/`

### Installing in IntelliJ IDEA

1. Go to `File → Settings → Plugins`
2. Click the gear icon and select `Install Plugin from Disk...`
3. Select the built plugin ZIP file
4. Restart IntelliJ IDEA

## Usage

Once installed, the plugin automatically checks your Java code for layer violations:

### Real-time Inspection
- Violations are highlighted with red underlines as you type
- Error messages explain the specific violation
- Hover over violations to see detailed descriptions

### Quick Fixes
Each violation includes quick fixes that provide guidance on how to resolve the issue:
- **Move to DTO Layer**: For controller violations
- **Use FLOW Layer**: For API-to-API communication
- **Extract Business Logic**: For logic in wrong layers
- **Create DAO Method**: For data access violations

### Configuration

The plugin works out of the box with default naming conventions. It detects layers based on:
- Class name suffixes (Controller, DTO, Service, Flow, DAO, etc.)
- Package names (controller, dto, service, flow, dao, etc.)
- Annotations (@Controller, @Service, @Repository, etc.)

## Examples

### ✅ Correct Usage

```java
// Controller - only calls DTO
@RestController
public class UserController {
    private UserDTO userDTO;
    
    public ResponseEntity<User> getUser(Long id) {
        return ResponseEntity.ok(userDTO.getUser(id));
    }
}

// DTO - calls API layer
public class UserDTO {
    private UserService userService;
    
    public User getUser(Long id) {
        // Validation and conversion
        validateId(id);
        return userService.findUser(id);
    }
}

// API/Service - contains business logic
@Service
public class UserService {
    private UserDAO userDAO;
    private UserFlow userFlow; // For complex operations
    
    public User findUser(Long id) {
        // Business logic here
        return userDAO.findById(id);
    }
}

// FLOW - orchestrates multiple APIs
public class UserFlow {
    private UserService userService;
    private NotificationService notificationService;
    
    public void processUserRegistration(User user) {
        userService.createUser(user);
        notificationService.sendWelcomeEmail(user);
    }
}

// DAO - database operations only
@Repository
public class UserDAO {
    public User findById(Long id) {
        // Database query
        return entityManager.find(User.class, id);
    }
}
```

### ❌ Violations Detected

```java
// ❌ Controller calling API directly
@RestController
public class UserController {
    private UserService userService; // Should use DTO instead
}

// ❌ DTO with business logic
public class UserDTO {
    public User processUser(User user) {
        // ❌ Complex business logic should be in API layer
        if (user.getAge() > 65) {
            user.setDiscount(0.15);
            calculatePremium(user);
        }
        return user;
    }
}

// ❌ API calling another API directly
@Service
public class UserService {
    private OrderService orderService; // ❌ Should use FLOW layer
}

// ❌ DAO with business logic
@Repository
public class UserDAO {
    public User findUser(Long id) {
        User user = findById(id);
        // ❌ Business logic should be in API layer
        if (user.isActive()) {
            user.updateLastAccessed();
        }
        return user;
    }
}
```

## Supported Annotations

The plugin recognizes these common annotations for layer detection:
- `@RestController`, `@Controller` → Controller layer
- `@Service` → API layer  
- `@Repository`, `@Entity`, `@Table` → DAO layer

## Development

### Project Structure
```
src/main/kotlin/com/layerdog/
├── inspections/           # Inspection implementations
│   ├── BaseLayerInspection.kt
│   ├── ControllerLayerInspection.kt
│   ├── DTOLayerInspection.kt
│   ├── APILayerInspection.kt
│   ├── FlowLayerInspection.kt
│   └── DAOLayerInspection.kt
└── utils/
    └── LayerDetector.kt   # Layer detection utility
```

### Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and feature requests, please create an issue in the GitHub repository.
