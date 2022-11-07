.. _tmPush:

Tigase Messenger Push Notifications
====================================

This section contains information about advanced settings and options that are available to the application, but may not be typically considered for users.

Push Component Support
-----------------------

Tigase Messenger for Android supports `XEP-0357 Push Notifications <https://xmpp.org/extensions/xep-0357.html>`__ which will receive notifications even when the program is closed from inactivity or memory management.

.. tip::

   Push notifications will not work if the user is offline.

Servers will be checked for Push support in the accounts management section of the application. If it is supported, you will be able to enable push notifications.

Devices must be registered for push notifications and must register them VIA the Tigase XMPP Push Component.

This can be done by activating Push support in the account management section. Tigase push component has a specific ad-hoc command known as ``register-device`` for this purpose.

Deregistration of a device may be accomplished by a similar ad-hoc command, using the ``unregister-device`` command on the Tigase push component. To send this, disable push notifications on your device.

For more information on the push component and its' capabilities, visit `push component documentation <http://docs.tigase.org/tigase-push/snapshot/Tigase_Push_Notifications_Guide/html/>`__.

.. note::

   Push functionality is ONLY available on Tigase services, or servers running a probably configured Tigase Push component.


Links
------

-  Drop by our `forum <https://projects.tigase.org/projects/tigase-mobilemessenger/boards>`__ to discuss **Tigase Messenger**.

-  Our other `projects <https://projects.tigase.org/>`__.
