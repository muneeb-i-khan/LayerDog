# LayerDog Rule Configuration

LayerDog now supports dynamic rule configuration through external JSON files! This allows you to customize architectural rules without modifying the plugin code. Additionally, LayerDog supports custom sound effects that play when you hover over violations!

## Getting Started

### 1. Initialize External Configuration

1. In IntelliJ IDEA, go to **Tools > LayerDog > Initialize Rule Configuration**
2. This creates a configuration file at `~/.layerdog/layer-rules.json`
3. The file will open in your default editor

### 2. Customize Your Rules

Edit the `layer-rules.json` file to customize:

- **Layer Detection Rules**: How classes are categorized into layers
- **Layer Interaction Rules**: Which layers can call which layers
- **Business Logic Detection**: Patterns that indicate business logic violations
- **Violation Messages**: Custom error messages for different violations
- **Quick Fix Suggestions**: Guidance provided to developers

### 3. Runtime Updates

Changes to the configuration file are automatically detected and reloaded! No need to restart IntelliJ.

## Configuration Structure

### Layer Definitions

Each layer can be configured with:

```json
{
  "layers": {
    "CONTROLLER": {
      "name": "Controller",
      "description": "Should have no business logic and only call DTO layer",
      "detection": {
        "classNamePatterns": {
          "suffixes": ["Controller", "Resource", "Endpoint"],
          "prefixes": [],
          "contains": []
        },
        "packagePatterns": {
          "contains": ["controller", "web", "rest"],
          "exact": []
        },
        "annotations": ["Controller", "RestController"]
      },
      "allowedCalls": ["DTO"],
      "rules": {
        "businessLogicProhibited": true,
        "businessLogicMessage": "Controller '{className}' contains business logic. Controllers should delegate to DTOs."
      }
    }
  }
}
```

### Global Rules

Configure global detection patterns:

```json
{
  "globalRules": {
    "businessLogicDetection": {
      "complexLogicThreshold": {
        "ifStatements": 2,
        "switchStatements": 2,
        "loopStatements": 2
      },
      "calculationLogicThreshold": {
        "assignmentExpressions": 3,
        "binaryExpressions": 5
      }
    },
    "javaLibraryPackages": ["java.", "javax.", "com.sun."],
    "databaseRelatedPatterns": {
      "classNames": ["Connection", "DataSource", "Driver"],
      "packages": ["java.sql", "javax.sql", "hibernate"]
    }
  }
}
```

### Custom Messages

Customize violation messages:

```json
{
  "violationMessages": {
    "invalidLayerCall": {
      "CONTROLLER_TO_API": "Controller '{fromClass}' is directly calling API layer '{toClass}'. Use DTO layer instead.",
      "GENERIC": "{fromLayer} '{fromClass}' is calling '{toClass}' which violates layer architecture."
    }
  }
}
```

### Quick Fixes

Customize quick fix suggestions:

```json
{
  "quickFixes": {
    "extractBusinessLogic": {
      "name": "Extract business logic to API layer",
      "description": "To fix this violation:\n1. Create a new service/API class\n2. Move the business logic methods to the API class\n3. Inject the API class into the controller\n4. Update the controller to call the API methods"
    }
  }
}
```

## Sound Configuration

### Setting Up Sound Effects

LayerDog can play custom sounds when you hover over architectural violations! Here's how to set it up:

1. **Initialize Configuration**: Go to **Tools > LayerDog > Initialize Rule Configuration**
2. **Configure Sounds**: Go to **Tools > LayerDog > Configure Sounds**
3. **Select Sound File**: Choose a `.wav`, `.au`, or `.aiff` file
4. **Enable Sounds**: Edit the configuration file to enable sound effects

### Sound Configuration Options

```json
{
  "globalRules": {
    "soundConfiguration": {
      "enabled": true,
      "soundFile": "/path/to/your/sound.wav",
      "volume": 0.7,
      "playOnHover": true,
      "playOnInspection": false,
      "debounceMs": 1000
    }
  }
}
```

**Configuration Options:**
- `enabled`: Turn sound effects on/off
- `soundFile`: Path to your custom audio file (leave empty for system beep)
- `volume`: Volume level (0.0 to 1.0)
- `playOnHover`: Play sound when hovering over violations (recommended)
- `playOnInspection`: Play sound when violations are first detected
- `debounceMs`: Minimum time between sounds to avoid spam

### Supported Audio Formats

- **WAV** (.wav) - Recommended
- **AU** (.au) - Sun Audio format  
- **AIFF** (.aif, .aiff) - Audio Interchange File Format

### Sound Examples

**For subtle notifications:**
- System notification sounds
- Soft chimes or bells
- Short musical notes

**For more attention-grabbing:**
- Alert sounds
- Buzzer sounds
- Custom voice recordings

### Testing Your Sounds

Use **Tools > LayerDog > Configure Sounds > Test Current Sound** to preview your audio before enabling it.

## Common Customizations

### Adding New Layer Types

1. Add a new layer definition in the `layers` section
2. Define detection patterns (class names, packages, annotations)
3. Specify which layers it can call
4. Add any specific rules

### Custom Package Conventions

Update the `packagePatterns` for each layer to match your project structure:

```json
{
  "detection": {
    "packagePatterns": {
      "contains": ["com.mycompany.controllers", "web.controllers"],
      "exact": ["com.mycompany.web"]
    }
  }
}
```

### Framework-Specific Annotations

Add annotations specific to your framework:

```json
{
  "detection": {
    "annotations": [
      "org.springframework.web.bind.annotation.RestController",
      "javax.ws.rs.Path",
      "io.micronaut.http.annotation.Controller"
    ]
  }
}
```

### Custom Business Logic Patterns

Add method name patterns that indicate business logic in your domain:

```json
{
  "rules": {
    "businessLogicPatterns": [
      "calculate", "process", "transform", 
      "validate", "approve", "reject",
      "reconcile", "aggregate"
    ]
  }
}
```

## File Location

The configuration file is located at:
- **macOS/Linux**: `~/.layerdog/layer-rules.json`  
- **Windows**: `%USERPROFILE%\.layerdog\layer-rules.json`

## Troubleshooting

### Configuration Not Loading

1. Check the IntelliJ logs for error messages
2. Verify the JSON syntax is valid
3. Ensure the file permissions allow reading
4. Try resetting to defaults using the Tools menu

### Rules Not Working as Expected

1. Check that your class names/packages match the detection patterns
2. Verify the layer interaction rules are correctly defined
3. Test with a simple example first

### File Watching Issues

If changes aren't being detected automatically:
1. Restart IntelliJ IDEA
2. Check that the file path hasn't changed
3. Verify file system permissions

### Sound Issues

**Sounds Not Playing:**
1. Verify `"enabled": true` in soundConfiguration
2. Check that the sound file exists at the specified path
3. Test with system beep by leaving soundFile empty
4. Check volume levels on your system

**Sounds Playing Too Often:**
1. Increase `debounceMs` value (try 2000-5000)
2. Set `playOnInspection`: false if only want hover sounds

**Unsupported Audio Format:**
1. Convert your audio to WAV format
2. Use shorter audio files (< 5 seconds recommended)
3. Try lower bitrate/sample rate if file won't load

## Support

For issues or questions about rule configuration, please check the project documentation or create an issue in the GitHub repository.
