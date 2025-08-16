# Portuguese Localization Setup

This project now supports Portuguese language localization alongside English.

## 📁 File Structure

```
app/src/main/res/
├── values/
│   └── strings.xml          # English strings (default)
└── values-pt/
    └── strings.xml          # Portuguese strings
```

## 🌐 How It Works

### Automatic Language Detection
- The app automatically detects the device's language setting
- If the device is set to Portuguese, it will use `values-pt/strings.xml`
- If the device is set to any other language, it will use `values/strings.xml` (English)

### String Resources
All user-facing text in the app is now localized:
- **UI Labels**: Buttons, titles, descriptions
- **Dialog Messages**: Confirmations, errors, information
- **Content Descriptions**: Accessibility labels for screen readers
- **Settings**: Configuration options and descriptions

## 🔧 Implementation Details

### Compose UI
The app uses `stringResource()` calls throughout the Compose UI:
```kotlin
Text(stringResource(R.string.app_name))
Text(stringResource(R.string.settings))
```

### Android Manifest
The app name is localized:
```xml
android:label="@string/app_name"
```

## 📝 Adding New Strings

### 1. Add to English (values/strings.xml)
```xml
<string name="new_feature_title">New Feature</string>
```

### 2. Add to Portuguese (values-pt/strings.xml)
```xml
<string name="new_feature_title">Nova Funcionalidade</string>
```

### 3. Use in Code
```kotlin
Text(stringResource(R.string.new_feature_title))
```

## 🌍 Supported Languages

| Language | Code | File | Status |
|----------|------|------|---------|
| English | `en` | `values/strings.xml` | ✅ Complete |
| Portuguese | `pt` | `values-pt/strings.xml` | ✅ Complete |

## 🚀 Adding More Languages

To add another language (e.g., Spanish):

1. Create `app/src/main/res/values-es/strings.xml`
2. Copy all strings from `values/strings.xml`
3. Translate each string to Spanish
4. The app will automatically support Spanish when the device language is set to Spanish

## 📱 Testing Localization

### Method 1: Device Settings
1. Go to Device Settings → System → Languages & input → Languages
2. Add Portuguese and move it to the top
3. Restart the app
4. All text should now appear in Portuguese

### Method 2: Android Studio
1. Open Android Studio
2. Go to Run → Edit Configurations
3. In "Launch Options", set "Language" to "pt"
4. Run the app - it will launch in Portuguese

### Method 3: ADB Command
```bash
adb shell setprop persist.sys.language pt
adb shell setprop persist.sys.country BR
```

## 🔍 Common Issues

### Missing Translations
If a string is missing from the Portuguese file:
- The app will fall back to English
- Check the logcat for warnings about missing resources

### String Format Issues
Some strings might need special handling for different languages:
- Date formats
- Number formats
- Pluralization rules

### RTL Support
Portuguese is left-to-right (LTR), so no special RTL handling is needed.

## 📚 Best Practices

1. **Never hardcode strings** - Always use `stringResource()`
2. **Keep translations up to date** - Update both files when adding new features
3. **Test on real devices** - Emulator language switching can be unreliable
4. **Use descriptive names** - String keys should be self-explanatory
5. **Group related strings** - Use comments to organize strings by feature

## 🛠️ Maintenance

### Regular Tasks
- Review new strings added to English
- Translate new strings to Portuguese
- Test both languages regularly
- Update this documentation when adding new languages

### Quality Assurance
- Have native speakers review translations
- Test on different Android versions
- Verify accessibility with screen readers
- Check for text overflow in different languages

## 📖 Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Android String Resources](https://developer.android.com/guide/topics/resources/string-resource)
- [Compose Localization](https://developer.android.com/jetpack/compose/localization)
- [Material Design Localization](https://material.io/design/usability/bidirectionality.html)
