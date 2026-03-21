# Alfresco AIO Project - SDK 4.11.0

This is an All-In-One (AIO) project for Alfresco SDK 4.11.0.

## Main Purpose

This project customizes an Alfresco Content Services + Share deployment for ACIB workflows. Its main goal is to:

* provide custom repository webscripts for workflow/document operations (approved files listing/export, task info, version removal, deleted document reporting, and content copy from a source sibling),
* provide Share UI extensions to expose those operations in the user interface,
* deliver a Docker-based local environment for development and integration testing.

Run with `./run.sh build_start` or `./run.bat build_start` and verify that it

 * Runs Alfresco Content Service (ACS)
 * Runs Alfresco Share
 * Runs Alfresco Search Service (ASS)
 * Runs PostgreSQL database
 * Deploys the JAR assembled modules
 
All the services of the project are now run as docker containers. The run script offers the next tasks:

 * `build_start`. Build the whole project, recreate the ACS and Share docker images, start the dockerised environment composed by ACS, Share, ASS and 
 PostgreSQL and tail the logs of all the containers.
 * `build_start_it_supported`. Build the whole project including dependencies required for IT execution, recreate the ACS and Share docker images, start the 
 dockerised environment composed by ACS, Share, ASS and PostgreSQL and tail the logs of all the containers.
 * `start`. Start the dockerised environment without building the project and tail the logs of all the containers.
 * `stop`. Stop the dockerised environment.
 * `purge`. Stop the dockerised container and delete all the persistent data (docker volumes).
 * `tail`. Tail the logs of all the containers.
 * `reload_share`. Build the Share module, recreate the Share docker image and restart the Share container.
 * `reload_acs`. Build the ACS module, recreate the ACS docker image and restart the ACS container.
 * `build_test`. Build the whole project, recreate the ACS and Share docker images, start the dockerised environment, execute the integration tests from the
 `integration-tests` module and stop the environment.
 * `test`. Execute the integration tests (the environment must be already started).

## Webscripts

### Repository Webscripts (`acibfunin-platform`)

* `GET /tasks/files/approved`: Returns the list of files approved in workflows (optionally filtered by year and username) as JSON entries for Share pages and client scripts.
* `GET /tasks/files/approved/export`: Exports approved workflow files to CSV (with localized column headers), used by the Share profile toolbar/page action.
* `GET /task/info?taskId={taskId}`: Returns detailed file/task information for one workflow task, used by custom workflow form controls.
* `GET /documents/deleted`: Returns archived documents from the trashcan, split into `replaced` and `deleted`, with original path and deletion date.
* `POST /api/node/version/{store_type}/{store_id}/{id}`: Removes a specific version label from a node version history.
* `POST /com/acibfunin/content/copy`: Copies binary content from sibling `documento-fuente` to a target node, creates a dated backup copy, and tries to avoid version auto-increment during replacement.

### Share Component Webscripts (`acibfunin-share`)

* `GET /components/approvedTasksList/body`: Controller/view for the custom Approved Tasks page body. It loads approved entries, normalizes task IDs, and enables user filtering controls for quality-control users.
* `GET /components/workflow/workflow-form`: Customized workflow form component for task/workflow details and custom form rendering.

### Share Overrides With Webscript Templates

These are Share webscript template overrides used via extension modules:

* `components/document-details/document-versions.get.html.ftl`: Injects custom JS/CSS for the version-removal button in Document Details.
* `components/profile/userprofiletoolbar.get.js`: Adds a link to `approvedTasksList` in the user profile toolbar.
* `components/head/resources.get.html.ftl`: Injects custom login CSS resources.
* `components/form/controls/workflow/packageItems.ftl`: Replaces package items rendering to fetch task file data from `/task/info`.
* `components/form/controls/workflow/activiti-transitions.ftl`: Customizes workflow transition button label/default for approve action.

## Share UI Customizations

The Share module (`acibfunin-share`) includes these UI customizations:

* Login styling and branding via `META-INF/login/customizations/components/head/resources.css` and custom logos in `META-INF/login/customizations/components/head/images/`.
* Profile toolbar extension that adds "Approved Tasks List" for the active user profile.
* Custom `approvedTasksList` page (page/template/component definitions, year/user filtering, and CSV export integration).
* Workflow form override with custom controls for package items and transitions.
* Document Versions override adding a Remove Version action that calls the repository remove-version webscript.
* Custom theme definition (`greenTheme`) and associated image assets.

# Few things to notice

 * No parent pom
 * No WAR projects, the jars are included in the custom docker images
 * No runner project - the Alfresco environment is now managed through [Docker](https://www.docker.com/)
 * Standard JAR packaging and layout
 * Works seamlessly with Eclipse and IntelliJ IDEA
 * JRebel for hot reloading, JRebel maven plugin for generating rebel.xml [JRebel integration documentation]
 * AMP as an assembly
 * Persistent test data through restart thanks to the use of Docker volumes for ACS, ASS and database data
 * Integration tests module to execute tests against the final environment (dockerised)
 * Resources loaded from META-INF
 * Web Fragment (this includes a sample servlet configured via web fragment)

# TODO

  * Abstract assembly into a dependency so we don't have to ship the assembly in the archetype
  * Functional/remote unit tests
