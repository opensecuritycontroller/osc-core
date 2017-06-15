# OSC Server

This project contains all business logic for OSC and provides implementation for [**osc-service-api**](../osc-service-api). **osc-server** validates inputs and persists the entities in the database through the [**osc-domain**](../osc-domain) project. This project also asynchronously updates the following external services: virtualization platforms, security managers, and SDN controllers.

### Dependencies
This project depends on [**osc-common**](../osc-common), [**osc-domain**](../osc-domain), [**osc-service-api**](../osc-service-api), [**sdn-controller-api**](https://github.com/opensecuritycontroller/sdn-controller-api), and [**security-manager-api**](https://github.com/opensecuritycontroller/security-mgr-api).

