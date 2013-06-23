# JUnit Test Utilities

## Overview

This project provides utilities for testing using junit. 
In the moment it is included: 
	AsyncTestRunner - A test runner to enable testing of asynchronous Tests 
	
## Getting Started

### Prerequisites

#### Build the project
This project is built with maven. You should install maven on your operating system to be able to build the project. 
Use 'mvn -install' in the root directory of the project (the project's POM file is located there). This will install a jar of this project in your local maven repository.
After a successful building process it will generate a jar file in the "target" folder, containing the compiled classes.

## Usage 

Include the junit-test-utilities-<version>.jar in your classpath. 
If you create a maven project, it is possible to include the installed annotation processor in the dependency section:

		 <dependency>
			<groupId>de.tiq</groupId>
			<artifactId>junit-test-utilities</artifactId>
			<version>0.0.2-SNAPSHOT</version>
		</dependency>


Currently, you can use the following test utilities:

### AsncTestRunner

The AsyncTestRunner class extends the junit4 test runner class and is designed for simple (integration) testing using multiple threads.
Of course testing using threads isn't best practice, but in the integration test phase it is sometimes not avoidable. 
There is often an issue that junit isn't able to collect exceptions from treads:

```java
Class TestClass
{
		//... includes omitted
		@Test
		public void testSomething(){
			//... building your test
			new Thread(){
				@Override
				public void run(){
					throw new RuntimeException("junit will never know!");
				}
			}.start();
		}
		
}
```
In this case you can use the __AsyncTestRunner.class__ together with the __@RunWith__ annotation provided by junit to mark your test class doing something asynchronously.

```java
@RunWith(AsyncTestRunner.class)
Class TestClass
{	
	//...
}
```

But what if the testable threads are still running after the execution of the test method? 

The current AsyncTestRunner will wait for their termination until a *specified timeout (3 sec)* is exeeded. 
If the timeout is overstepped, an __ThreadsStillAliveException__ is thrown. 
Note: The threads will not be termined here. This should be a task of the @After function! Maybe for later releases this feature could be feasable to implement.
However, this mechanism will not be applied to deamon treads.

But what if the timeout duration is not enough for the shutdown phase of your threads? 
You can specify your own timeout length using the __@ThreadShutdownTimeout__ annotation: 
The value of this annotation will be used for the duration of the timeout in milliseconds.
```java
@RunWith(AsyncTestRunner.class)
Class TestClass
{
	//... includes omitted
	
		@ThreadShutdownTimeout(5000)
		@Test
		public void testSomethingWithTimeoutOfThreads(){
			//... building your test
			new Thread(){
				@Override
				public void run(){
					try{
						Thread.sleep(4000);
						throw new RuntimeException("I need a longer timeout!");
					} catch(Exception e){
						//...
					}
				}
			}.start();
		}
}
```
#### Expected Exceptions

You can use the __@Test(expected=Exception.class)__ annotation like you're used to.
The test runner can produce two exceptions, which you should probaly know in this context:

	__ThreadsStillAliveException__ - is thrown when a thread was not terminated
	__AsynchronousTestRunnerException__ - is thrown when there are more than two threads of the testmethod have thrown a exception
		
## Developer info

The project is in beta state. Some tests are failing time to time. 
Many more interessting junit features should be added here. 
I hope for contribution!

## Colophon		

### License 

The project is licensed under terms of the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
