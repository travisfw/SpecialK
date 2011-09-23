package com.biosimilarity.lift.model.store

import scala.xml._

/* User: jklassen
*/

trait Persist
{
  def checkIfDBExists(collectionName: String, leaveOpen: Boolean): Boolean

  def open(collectionName: String, retries: Int, wait: Int) : Any

  def drop(collectionName: String)

  //is read necessary? is it really read by query instead of by key?
  def read(collectionName: String, key: String)

  def insertUpdate(collectionName: String, key: String, value: String)

  def delete(collectionName: String, key: String)


  def execute(query: String): Unit

  def execute(queries: List[String]): Unit

  def executeScalar(query: String): String

  def executeWithResults(query: String): List[Elem]

  def executeWithResults(queries: List[String]): List[Elem]

  //def count
}