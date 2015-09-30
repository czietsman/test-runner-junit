# test-runner-junit
Tool to execute junit tests

## Usage
Copy all application and junit test jars into one or more directories.

```
test-runner-junit --help
```

### Run all tests

```
test-runner-junit <folders>
```

### Run only tests from certain jars

```
test-runner-junit <folders> --jar-suffix "-integTests.jar"
```

### Use categories to run only some tests

This leverages Junit Categories to pick tests based on the categories

```
test-runner-junit --include com.redstor.RedTest --include com.redstor.GreenTest --exclude com.redstor.YellowTest <folders>
```
