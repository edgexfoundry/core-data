# v0.2 (10/20/2017)
# Release Notes

## Notable Changes
The Barcelona Release (v 0.2) of the Core Data micro service includes the following:
* Application of Google Style Guidelines to the code base
* Increase in unit/intergration tests from 630 tests to 965 tests
* POM changes for appropriate repository information for distribution/repos management, checkstyle plugins, etc.
* Removed all references to unfinished DeviceManager work as part of Dell Fuse
* Added Dockerfile for creation of micro service targeted for ARM64 
* Added interfaces for all Controller classes

## Bug Fixes
* Fix GET events by device with device id
* Removed OS specific file path for logging file 
* Provide option to include stack trace in log outputs

## Pull Request/Commit Details
 - [#17](https://github.com/edgexfoundry/core-data/pull/17) - Remove staging plugin contributed by Jeremy Phelps ([JPWKU](https://github.com/JPWKU))
 - [#16](https://github.com/edgexfoundry/core-data/pull/16) - Fixes POM Client Version Reference contributed by Tyler Cox ([trcox](https://github.com/trcox))
 - [#15](https://github.com/edgexfoundry/core-data/issues/15) - Minor - POM property should reference <core-metadata-client> (line 37)?
 - [#14](https://github.com/edgexfoundry/core-data/pull/14) - Fixes Maven artifact dependency path contributed by Tyler Cox ([trcox](https://github.com/trcox))
 - [#13](https://github.com/edgexfoundry/core-data/pull/13) - added staging and snapshots repos to pom along with nexus staging mav… contributed by Jim White ([jpwhitemn](https://github.com/jpwhitemn))
 - [#12](https://github.com/edgexfoundry/core-data/pull/12) - Removed device manager url refs in properties files contributed by Jim White ([jpwhitemn](https://github.com/jpwhitemn))
 - [#11](https://github.com/edgexfoundry/core-data/pull/11) - Adding docker for aarch64 contributed by ([feclare](https://github.com/feclare))
 - [#10](https://github.com/edgexfoundry/core-data/pull/10) - previous commit seemed to miss addition of repackaging, new tests, an… contributed by Jim White ([jpwhitemn](https://github.com/jpwhitemn))
 - [#9](https://github.com/edgexfoundry/core-data/pull/9) - google style codes, check styles, pom changes, print stack trace resolution with REST calls, unit unit tests, test suites, sonal lint clean up contributed by Jim White ([jpwhitemn](https://github.com/jpwhitemn))
 - [#8](https://github.com/edgexfoundry/core-data/pull/8) - Added 'findByDevice' contributed by Soumya Kanti Roy chowdhury ([soumyakantiroychowdhury](https://github.com/soumyakantiroychowdhury))
 - [#7](https://github.com/edgexfoundry/core-data/pull/7) - Adds Docker build capability contributed by Tyler Cox ([trcox](https://github.com/trcox))
 - [#6](https://github.com/edgexfoundry/core-data/issues/6) - API for core data /event/device/{deviceId}/{limit}: does not work with deviceId but deviceName +fix
 - [#5](https://github.com/edgexfoundry/core-data/pull/5) - Fixes Log File Path contributed by Tyler Cox ([trcox](https://github.com/trcox))
 - [#4](https://github.com/edgexfoundry/core-data/issues/4) - Log File Path not Platform agnostic
 - [#3](https://github.com/edgexfoundry/core-data/pull/3) - Add distributionManagement for artifact storage contributed by Andrew Grimberg ([tykeal](https://github.com/tykeal))
 - [#2](https://github.com/edgexfoundry/core-data/pull/2) - Fixes ZeroMQ Socket Multi-Threading contributed by Tyler Cox ([trcox](https://github.com/trcox))
 - [#1](https://github.com/edgexfoundry/core-data/pull/1) - Contributed Project Fuse source code contributed by Tyler Cox ([trcox](https://github.com/trcox))
