# rolesmanager Changelog

## 0.1.0

### New Features

* Bumped jahia-parent from 8.0.0.0 to 8.1.9.0 (#117)

### Bug Fixes

* Fixed permission tree display to correctly mark child permissions as active when their parent permission is granted (e.g. `jcr:write_default` children). Previously, saving a role without changes could silently strip implicitly inherited permissions from the role configuration (#115)
