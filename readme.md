[![Run Status](https://api.shippable.com/projects/5756ccf92a8192902e22c72c/badge?branch=master)](https://app.shippable.com/projects/5756ccf92a8192902e22c72c) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/09d679eb62f64a87ad7a9bfc90c643cc)](https://www.codacy.com/app/MUSIT-Norway/musit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MUSIT-Norway/musit&amp;utm_campaign=Badge_Coverage) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/09d679eb62f64a87ad7a9bfc90c643cc)](https://www.codacy.com/app/MUSIT-Norway/musit?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MUSIT-Norway/musit&amp;utm_campaign=Badge_Grade)

# MUSIT 
Norwegian university museums IT organization (MUSIT); cultural history and natural history database with store integration.

MUSIT is a cooperation between the University of Oslo, University of Bergen, Norwegian University of Science and Technology, University of Tromsø - Norway's Arctic university, and the University of Stavanger. 
The University museums of Norway hold in trust regional and global scientific collections of natural and cultural history. The cooperative aims to ensure the operation, maintenance and development of the university museums' joint collection databases, and to facilitate sharing dissemination of data for research, education, management and the public. 

MUSIT strives for greater integration between its databases. And is an open source initiative cooperating with other projects such as DINA (National History museum of Stockholm - Sweden), and Kotka (National History museum of Helsinki - Finland).


# Usage

Run 'Main' in IDEA. You will be prompted for endpoint, session token, input file path and museum id.

Endpoint: Copy the endpoint from the address field in the browser and paste it after the prompt in IDEA. For example http://musit-test:8888 or https://musit-utv.uio.no. Edit the link after pasting, for example by deleting and retyping the last character, to prevent IDEA interpreting the input as a link and opening it in the browser. Press enter.
 
Session token:  Open the endpoint in the browser. Open developer tools in the browser, open the Network pane. Select the storage node where we want to add new nodes. In developer tools, select the request for the storage node, find the Authorization key in Request Headers under Headers. Copy the value (starting with 'Bearer') into IDEA and press enter. 

Input file path: Copy full path, including filename, to the file with nodes to be inserted. The file must be in csv format using ; as column separator.
First column holds the id for the node where the storage nodes will be added. The id can be found in the response for the storage node in developer tools. In the example below, the id value is 5: 
{"id":5,"nodeId":"0113...,... }. The other columns hold the names for the storage nodes, each column defining a subnode of previous column, like this:
5;Reol 1;Seksjon 2;Hylle 1
5;Reol 1;Seksjon 2;Hylle 2
5;Reol 1;Seksjon 2;Hylle 3

Museum id: The museum id can be found in developer tools in the Request Url after museum/. For example, museum id = 99 below:
http://musit-test:8888/api/storagefacility/museum/99/storagenodes/011...

Note that session token, id for the storage node used in the csv file and museum number must be copied for each endpoint. Create separate import files for utv, test and prod, with corresponding node id in the first column. 


# Contribution

## Prerequisites

To get started you need to install the following components on your computer:

* [SBT](http://www.scala-sbt.org) latest version.
* [Java 8](http://java.oracle.com) latest patchlevel of java 8 SE JDK. (Open JDK should also work)

# License
All code is protected under the [GPL v2 or later](http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html) and copyright holder is [MUSIT](http://musit.uio.no) as a part of the [University of Oslo](http://www.uio.no).

# Contact

If you are interested in participating in the project please contact opensource@musit.uio.no (not active yet)


# Credits

<img src="https://raw.githubusercontent.com/MUSIT-Norway/guidelines/master/images/icon_IntelliJIDEA.png" alt="alt text" width="40px" height="40px"> We would like to thank [JetBrains](https://www.jetbrains.com) for free OSS licenses to [IntelliJ IDEA](https://www.jetbrains.com/idea/)

