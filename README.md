Domino-Cocomo
=============

Cocomo analysis for IBM Domino applications

Knowing how much it will take to **build new** software is one of the dark arts in Computer Science.
The university of Southern Californa is researching this topic for decades and has revised an
approach that centers around the Lines of Code (LOC). Based on the LOC and weighting factors
they are able to derive the time and staffing needed to complete a software project.

Read about their approach **Constructive Cost Model** here:

 - [Constructive Cost Model](http://en.wikipedia.org/wiki/COCOMO) on Wikipedi

While it might be challening to estimate *future* lines of code, it is comparable simple
to count existing lines of code.

The existing LOC count gives a reliable measure of the **replacement effort** for a given application.

In Domino that count isn't that simple, since some of the artefacts are created in a visual designer rather than in source code.
This little tool helps to generate the LOC equivalent for a Domino application and thus give a reliable measure of the current **value** of an application.

The tool is designed to analyse a bulk of applications in one go.

Usage:
------

 1. In Designer preferences uncheck *Use binary DXL for version control* - This ensures we can parse all the files
 2. Associate all Applications (NSF) with On-Disk-Projects. Those projects should be created in their own directory structure outside the workspace
 3. Use the JAR version of this code with <code>java -jar cocomo.jar Directory ReportFile.csv</code>
 4. Load the csv file in a spreadsheet editor of your choice. The last column will show the LOC equivalent for each app (one app per line)
 5. Go to the [CoCoMo Tooling](http://csse.usc.edu/tools/COCOMOII.php) and enter that number (or the total for all), add your developer's cost and see time money that is contained in that apps (what you would need to replace them)

Caveats:
--------

 - The simplified assumption here is, that your new target platform needs the same amount of code. From experience we know it is more on other platforms. On the other hand you might generate some of the code which is faster
 - The approach glosses over data migration efforts. Depending on a target platform, the transition from a NoSQL/Document (as in Domino) to an RDBMS or flat-table model can be more effort than transforming the code

   
