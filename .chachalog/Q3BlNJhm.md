---
rolesmanager: patch
---

Copying a role now runs on the caller session, the same way creating one already does, so both paths ask the operator for the same rights. The identifier submitted with the copy must designate a role stored under /roles; anything else is refused with a message.
