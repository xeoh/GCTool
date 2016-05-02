GCTool
======

GimChiTool helps to analyze logs of the CMS garbage collector.

Project Structure
-----------------

- common : contains shared models for other subprojects.
- parser : parses the text logs to the data models.
- analyzer : analyzes the data and make useful stats.
- api : API server to receive the logs.
- cli-client : A CLI-based client for uploading the logs.
