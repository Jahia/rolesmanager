# rolesmanager Changelog

## 0.0.1

* Fixed permission tree display to correctly mark child permissions as active when their parent permission is granted (e.g. `jcr:write_default` children). Previously, saving a role without changes could silently strip implicitly inherited permissions from the role configuration (#115)
