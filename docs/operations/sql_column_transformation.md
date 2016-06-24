---
layout: documentation
displayTitle: SQL Column Transformation
title: SQL Column Transformation
description: SQL Column Transformation
usesMathJax: true
includeOperationsMenu: true
---

Executes a
<a target="_blank" href="http://spark.apache.org/docs/{{ site.WORKFLOW_EXECUTOR_SPARK_VERSION }}/sql-programming-guide.html#sql">Spark SQL</a>
(enriched with some [User Defined Functions](../spark_sql_udf.html))
formula (as used in `SELECT` statement) provided by the user on a column (columns)
of [DataFrame](../classes/dataframe.html) connected to its input port.
Returns modified `DataFrame`.

Also returns a [Transformer](../classes/transformer.html) that can be later applied
to another `DataFrame` with a [Transform](transform.html) operation.

**Since**: Seahorse 1.1.0

## Input

<table>
<thead>
<tr>
<th style="width:15%">Port</th>
<th style="width:15%">Type Qualifier</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>0</code></td>
<td><code><a href="../classes/dataframe.html">DataFrame</a></code></td>
<td>The input <code>DataFrame</code>.</td>
</tr>
</tbody>
</table>

## Output

<table>
<thead>
<tr>
<th style="width:15%">Port</th>
<th style="width:15%">Type Qualifier</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>0</code></td><td>
<code><a href="../classes/dataframe.html">DataFrame</a></code></td>
<td>The results of the transformation.</td>
</tr>
<tr>
<td><code>1</code></td><td>
<code><a href="../classes/transformer.html">Transformer</a></code></td>
<td>The <code>transformer</code> that allows to apply the operation on another <code>DataFrame</code> using
<code><a href="transform.html">Transform</a></code>.</td>
</tr>
</tbody>
</table>

## Parameters

<table class="table">
<thead>
<tr>
<th style="width:15%">Name</th>
<th style="width:15%">Type</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>
  <tr>
    <td><code>input column</code></td>
    <td><code><a href="../parameter_types.html#single-column-selector">SingleColumnSelector</a></code></td>
    <td>The input column that can be accessed with <code>input column alias</code>.</td>
  </tr>
  <tr>
    <td><code>input column alias</code></td>
    <td><code><a href="../parameter_types.html#string">String</a></code></td>
    <td>The identifier that can be used in the <code>Spark SQL</code> formula
        (as used in <code>SELECT</code> statement) to refer the input column.</td>
  </tr>
  <tr>
    <td><code>formula</code></td>
    <td><code><a href="../parameter_types.html#string">String</a></code></td>
    <td>The <code>Spark SQL</code> formula (as used in <code>SELECT</code> statement).</td>
  </tr>
  <tr>
    <td><code>output column name</code></td>
    <td><code><a href="../parameter_types.html#string">String</a></code></td>
    <td>The name of the newly created column holding the result.</td>
  </tr>
</tbody>
</table>

{% markdown operations/examples/SqlColumnTransformation.md %}