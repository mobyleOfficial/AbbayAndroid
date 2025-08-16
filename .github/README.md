# GitHub Actions Workflows

This directory contains GitHub Actions workflow templates for your Android project.

## Available Workflows

### 1. `android-basic-ci.yml` (Recommended for starting)
A simple CI workflow that:
- Builds your project
- Runs unit tests
- Performs lint checks
- Uploads build artifacts

**Use this if you're new to GitHub Actions or want a simple setup.**

### 2. `android-ci.yml` (Advanced)
A comprehensive CI/CD workflow that includes:
- All features from basic CI
- Security dependency scanning
- Instrumented tests
- Automated releases with APK signing
- GitHub releases

**Use this when you're ready for production deployments.**

## Setup Instructions

### Basic Setup (Recommended)
1. Copy `android-basic-ci.yml` to `.github/workflows/ci.yml`
2. Commit and push to trigger the workflow
3. No additional configuration needed

### Advanced Setup
1. Copy `android-ci.yml` to `.github/workflows/ci.yml`
2. Set up GitHub Secrets for APK signing:
   - `SIGNING_KEY`: Your base64-encoded keystore file
   - `KEY_ALIAS`: Your keystore alias
   - `KEY_STORE_PASSWORD`: Your keystore password
   - `KEY_PASSWORD`: Your key password

## Required Secrets for Advanced Workflow

If using the advanced workflow, add these secrets in your GitHub repository:

1. Go to your repository → Settings → Secrets and variables → Actions
2. Add the following secrets:
   - `SIGNING_KEY`: Base64-encoded keystore file
   - `KEY_ALIAS`: Keystore alias
   - `KEY_STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password

## Converting Keystore to Base64

```bash
# On macOS/Linux
base64 -i your-keystore.jks | pbcopy

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks"))
```

## Customization

### Branch Names
Update the `branches` section in the workflow to match your repository:
```yaml
on:
  push:
    branches: [ main, master, develop ]  # Add your branch names
  pull_request:
    branches: [ main, master, develop ]
```

### Gradle Tasks
Modify the build steps to match your project needs:
```yaml
- name: Build with Gradle
  run: ./gradlew assembleDebug  # Instead of build
```

### Cache Keys
Adjust cache keys if you have specific caching requirements:
```yaml
key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

## Troubleshooting

### Common Issues

1. **Gradle daemon issues**: The workflow sets `GRADLE_OPTS` to disable the daemon
2. **Permission denied**: The workflow grants execute permission to `gradlew`
3. **Cache misses**: Check that your Gradle files haven't changed significantly

### Debugging

1. Check the Actions tab in your GitHub repository
2. Review workflow logs for specific error messages
3. Ensure all required files are committed (including `gradlew` and `gradle-wrapper.jar`)

## Next Steps

1. Start with the basic workflow
2. Test that builds and tests pass
3. Gradually add more features as needed
4. Consider adding code coverage reporting
5. Add deployment to testing environments

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/build/building-cmdline)
- [Gradle Performance](https://docs.gradle.org/current/userguide/performance.html)
