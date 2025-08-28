# LayerDog Sound Configuration Examples

This file provides examples of how to configure sound effects for LayerDog architectural violations.

## Quick Start

1. **Initialize Configuration**
   ```
   Tools > LayerDog > Initialize Rule Configuration
   ```

2. **Configure Sounds**
   ```
   Tools > LayerDog > Configure Sounds
   ```

3. **Enable in Configuration**
   Edit `~/.layerdog/layer-rules.json` and set:
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

## Configuration Examples

### Basic Setup (System Beep)

```json
{
  "soundConfiguration": {
    "enabled": true,
    "soundFile": "",
    "volume": 0.8,
    "playOnHover": true,
    "playOnInspection": false,
    "debounceMs": 1500
  }
}
```

### Custom Sound File

```json
{
  "soundConfiguration": {
    "enabled": true,
    "soundFile": "/Users/username/sounds/notification.wav",
    "volume": 0.6,
    "playOnHover": true,
    "playOnInspection": false,
    "debounceMs": 800
  }
}
```

### Aggressive Notifications

```json
{
  "soundConfiguration": {
    "enabled": true,
    "soundFile": "/path/to/alert.wav",
    "volume": 0.9,
    "playOnHover": true,
    "playOnInspection": true,
    "debounceMs": 500
  }
}
```

### Subtle Notifications

```json
{
  "soundConfiguration": {
    "enabled": true,
    "soundFile": "/path/to/soft-chime.wav",
    "volume": 0.3,
    "playOnHover": true,
    "playOnInspection": false,
    "debounceMs": 2000
  }
}
```

## Sound File Recommendations

### For Development Teams
- **Soft Bell**: Gentle notification without disruption
- **Short Chime**: Professional, non-intrusive
- **Subtle Click**: Minimal audio feedback

### For Personal Use
- **Custom Voice**: "Layer violation detected"
- **Funny Sounds**: Make violations more memorable
- **Musical Notes**: Pleasant audio feedback

### Example Sound Files (Create These)

**notification.wav** - A soft ding sound (0.5 seconds)
```
Can be created with any audio editing software
Recommended: 44.1kHz, 16-bit, mono
```

**violation-alert.wav** - A brief alert sound (0.3 seconds)
```
Higher pitch, attention-grabbing but brief
Good for catching violations quickly
```

**chime.wav** - Musical chime (1 second)
```
Harmonic, pleasant sound
Good for longer development sessions
```

## Platform-Specific Paths

### macOS
```json
{
  "soundFile": "/Users/username/Documents/sounds/violation.wav"
}
```

### Windows
```json
{
  "soundFile": "C:\\Users\\username\\Documents\\sounds\\violation.wav"
}
```

### Linux
```json
{
  "soundFile": "/home/username/sounds/violation.wav"
}
```

## Testing Your Configuration

1. **Test Sound File**
   ```
   Tools > LayerDog > Configure Sounds > Test Current Sound
   ```

2. **Create a Violation**
   - Write code that violates layer rules
   - Hover over the highlighted violation
   - Sound should play

3. **Adjust Settings**
   - Edit configuration file
   - Save to automatically reload
   - Test again

## Common Issues and Solutions

### Sound Not Playing
```json
// Make sure enabled is true
"enabled": true,

// Check file path exists
"soundFile": "/absolute/path/to/file.wav",

// Try system beep first
"soundFile": "",
```

### Too Many Sounds
```json
// Increase debounce time
"debounceMs": 3000,

// Disable inspection sounds
"playOnInspection": false
```

### Volume Issues
```json
// Adjust volume (0.0 to 1.0)
"volume": 0.5,

// Test with different values
"volume": 0.8
```

## Creating Your Own Sounds

### Using Audacity (Free)
1. Generate tone or record audio
2. Keep it short (< 2 seconds)
3. Export as WAV format
4. Use mono for smaller files

### Online Sound Libraries
- Freesound.org
- Zapsplat.com
- Notification sounds from system

### Voice Recordings
```
"Controller violation detected"
"Wrong layer call"
"Architecture violation"
```

Record with smartphone and convert to WAV format.

## Advanced Configuration

### Different Sounds per Violation Type
While not currently supported, you can request this feature. Currently all LayerDog violations use the same sound configuration.

### Integration with IDE Themes
Consider choosing sounds that match your IDE color scheme:
- Dark themes: Lower pitched, subtle sounds
- Light themes: Higher pitched, crisp sounds

### Team Consistency
Share your `layer-rules.json` file with your team to ensure everyone has the same sound feedback for violations.

## Troubleshooting

### File Format Issues
```bash
# Convert to WAV using ffmpeg
ffmpeg -i input.mp3 -acodec pcm_s16le -ac 1 -ar 44100 output.wav
```

### Permission Issues
```bash
# Check file permissions (macOS/Linux)
ls -la /path/to/your/sound.wav
chmod 644 /path/to/your/sound.wav
```

### Java Sound Issues
If custom sounds don't work, try:
1. System beep first (`"soundFile": ""`)
2. Different audio format
3. Restart IntelliJ IDEA
4. Check IntelliJ logs for errors
