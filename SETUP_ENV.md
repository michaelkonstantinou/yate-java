# ⚙️ Setup environmental variables

## Common non-intuitive variables

The two most important (and sometimes confusing) parameters are `DIR_PROMPTS` and `DIR_OUTPUT`.

**DIR_PROMPTS**

This variable should point to the absolute path of the prompts directory inside **THIS** repository.

Example:

DIR_PROMPTS="/path/to/yate-java/prompts/"


Make sure the path points directly to the prompts folder. This is where the tool looks for prompt templates used to generate tests.

**DIR_OUTPUT**

This variable defines where YATE will generate files to assist on its test generation process.
SHALL NOT be confused with the output directory of your generated tests. This variables is only for YATE's internal use

Example:

DIR_OUTPUT="/path/to/yate-java/output/"


If the output directory doesn’t exist yet, simply create it at the root of this repository:

```bash
mkdir output
```

Once both variables are set, the tool will correctly read from the prompts folder and use the output folder for its process.

## Language-specific variables

This repository is used to support java and kotlin, and its implementation is test-framework-agnostic. 
Therefore, you need to setup the programming language and the test framework of the generated test(s)
The following variables are language-specific: `LANG`, `TEST_FRAMEWORK`, `REQUIRED_IMPORTS`

**LANG**

Can be either Java or Kotlin

**TEST_FRAMEWORK**

Can be anything. So far, it has been tested with Junit5 and TestNG. This is simply used to inform the LLM which
test framework to use. However, there are no further actions taken be YATE to ensure that the generated test is complying 
with the provided test framework

**REQUIRED_IMPORTS**

In general, this can contain any import statements you believe are necessary to be included in any generated test.
HOWEVER, depending on the language of your choice, you may which to include or exclude the semicolon ";"

e.g. For Java, we used the following in our experiments:

```
REQUIRED_IMPORTS=import org.junit.Before;,import org.mockito.Mockito.*;,import org.junit.jupiter.api.Assertions.*;
```

But for Kotlin we excluded the semicolon as it is not typically used in kotlin

```
REQUIRED_IMPORTS=import org.junit.Before,import org.mockito.Mockito.*,import org.junit.jupiter.api.Assertions.*
```