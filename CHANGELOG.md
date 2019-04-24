<!--
  Copyright 2019 Google LLC

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unresolved]

 ### Security
 
 ### Added
  
 ### Changed
				
 ### Removed
				 
 ### Fixed

## [0.8] 2019-04-24

 ### Security
 
 ### Added
 - License headers that were missing.
 - xml-format-maven plugin for formatting pom.xml.
 - tidy-maven plugin for maintaining a consistent structure in pom.xml.
 - Support for Jenkins Configuration as Code plugin. Use the base64 encoded
   secret directly in the configuration. `filename` is optional but recommended
   if you plan to also use the Jenkins UI.
 - Test-only dependencies on the configuration-as-code, plain credentials
   and ssh-credentials plugins.
 - fmt-maven-plugin for automatically formatting java files when built.
  
 ### Changed
 - Use SecretBytes to store credentials, rather than files.
 - When uploaded through the UI, the file name serves as an identifier for the
   key when choosing whether to upload a new key. The file name will be as
   uploaded rather than a new randomly generated file name
 - Updated base plugin version to 3.36
 - Updated Java version to 8
 - Updated minimum Jenkins version to 2.60.3
 - Updated credentials plugin to 2.1.16
 - Updated maven-javadoc-plugin to 2.10.4
				
 ### Removed
 - Issue #49: find-bugs plugin (spot-bugs now inherited from parent plugin pom)
				 
 ### Fixed
 - Various issues related to the use of manually generated files
   - Not able to load files after system restart, leading to NPEs in some 
   - Not able to update permissions on created files, so fail to have a usable key.

## [0.7] - 2019-02-06

 ### Security
 
 ### Added
  
 ### Changed
	- Upgraded google.api.version to 1.24.1
	- Updated joda-time to 2.9.5
	- Added git ignore for intellij and emacs
	- Added travis file
				
 ### Removed
				 
 ### Fixed
 
