# rolesmanager Changelog

## 0.1.0

### New Features

* Bumped jahia-parent from 8.0.0.0 to 8.1.9.0 (#117)

### Bug Fixes

* Copying a role requires the same rights as creating one, and the identifier submitted with the copy must designate a role stored under /roles; anything else is refused with a message.

* Fixed display of role and permission names, titles and descriptions containing special characters in the Roles & Permissions administration screens.

* Fixed permission tree display to correctly mark child permissions as active when their parent permission is granted (e.g. `jcr:write_default` children). Previously, saving a role without changes could silently strip implicitly inherited permissions from the role configuration (#115)
