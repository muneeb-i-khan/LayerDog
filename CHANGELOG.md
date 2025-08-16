# Changelog

All notable changes to the Layer Dog plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.3] - 2025-08-16

### Changed
- **BREAKING**: Layer violations now show as **warnings** (yellow squiggles) instead of errors (red squiggles)
- Improved user experience - warnings don't interfere with compilation errors
- Better visual distinction between architectural guidelines and critical errors

### Fixed
- Fixed plugin configuration issue with message bundles
- Resolved "Can't find inspection description" error
- Improved plugin loading and registration

## [1.0.2] - 2025-08-16

### Fixed
- Updated compatibility to support IntelliJ IDEA 2024.2+ (build 252.*)
- Fixed plugin installation compatibility issues

## [1.0.1] - 2025-08-16

### Fixed
- Initial compatibility fix for IntelliJ IDEA versions

## [1.0.0] - 2025-08-16

### Added
- **Initial release** 🎉
- Real-time architectural layer violation detection
- Support for 5 architectural layers:
  - **Controller Layer**: Should only call DTO layer
  - **DTO Layer**: Should only call API layer  
  - **API Layer**: Should call DAO/FLOW layers, not other APIs
  - **FLOW Layer**: Should orchestrate multiple API calls
  - **DAO Layer**: Should only interact with database
- Smart layer detection based on:
  - Class naming conventions (Controller, Service, DAO, etc.)
  - Package structure (controller, api, dao packages)
  - Spring annotations (@RestController, @Service, @Repository)
- Quick fix suggestions for common violations
- Maven project compatibility
- IntelliJ IDEA 2023.2+ support

### Features
- **Layer Detection**: Automatic identification of architectural layers
- **Real-time Validation**: Violations highlighted as you type
- **Warning-level Highlighting**: Non-intrusive yellow squiggles  
- **Contextual Messages**: Clear violation descriptions with hover tooltips
- **Quick Fixes**: Actionable suggestions to resolve violations
- **Comprehensive Coverage**: All major architectural patterns supported

[unreleased]: https://github.com/muneeb-i-khan/LayerDog/compare/v1.0.3...HEAD
[1.0.3]: https://github.com/muneeb-i-khan/LayerDog/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/muneeb-i-khan/LayerDog/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/muneeb-i-khan/LayerDog/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/muneebk/LayerDog/releases/tag/v1.0.0
