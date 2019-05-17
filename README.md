Mean Partitions
Block-Block Matrix Multiplication of Sparse Matrix
Fall 2018 Project

Code author
-----------
Siddhant Benadikar,
Sarthak Kothari,
Sahil Gandhi,
Mustafa Kapadia.

Installation
------------
These components are installed:
- JDK 1.8
- Hadoop 2.9.1
- Maven
- AWS CLI (for EMR execution)

Environment
-----------
1) Example ~/.bash_aliases:
export JAVA_HOME=$(/usr/libexec/java_home)
export HADOOP_HOME=/usr/local/opt/hadoop
export YARN_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

2) Explicitly set JAVA_HOME in $HADOOP_HOME/etc/hadoop/hadoop-env.sh:
export JAVA_HOME=/usr/lib/jvm/java-8-oracle

Generating Data
---------------
Use the python script GenData.py with 4 arguments. 

python GenData.py fly dim1 dim2 density

It is recommended to keep the first argument as fly for dimensions > 10,000.


Execution
---------
All of the build & execution commands are organized in the Makefile.
1) Unzip project file.
2) Open command prompt.
3) Navigate to directory where project files unzipped.
4) We have two Makefiles, one for Cannons and BB respectively. Please change the name from
   Makefile_BB and Makefile_Cannon to Makefile and run make.
5) To change the partitions size you need to make the following changes to BBAlgorithm.java and CannonAlgorithm.java:
    In BBAlgorithm.java, change values of A_ROW, A_COL, B_ROW, B_COL, P_ROW, P_MID, P_COL
    Where P_ROW * P_MID * P_COL == the number of partitions you want to keep. (eg: For p=100, P_ROW=5, P_MID=4, P_COL=4)
6) Standalone Hadoop:
	make switch-standalone		-- set standalone Hadoop environment (execute once)
	make local
7) AWS EMR Hadoop: (you must configure the aws.* config parameters at top of Makefile)
	make upload-input-aws		-- only before first execution
	make aws					-- check for successful execution with web interface (aws.amazon.com)
	download-output-aws			-- after successful execution & termination
