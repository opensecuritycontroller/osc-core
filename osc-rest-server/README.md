# OSC REST Server

The **osc-rest-server** project implements the REST APIs supported by OSC using [**osc-service-api**](../osc-service-api) to define payloads and to interface with the OSC business logic. This project is a thin wrapper on top of the **[osc-server](../osc-server)** project and should not contain business logic.

### Dependencies
This project depends on [**osc-common**](../osc-common) and [**osc-service-api**](../osc-service-api).