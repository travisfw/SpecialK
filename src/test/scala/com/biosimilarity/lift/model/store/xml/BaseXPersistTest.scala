/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.biosimilarity.lift.model.store.xml

/* User: jklassen
 */

import org.specs._
import org.specs.util._
import org.specs.runner._
import scala.concurrent.ops._

import com.biosimilarity.lift.lib._

import scala.util._
import actors.Actor._

class MockBaseXPersist extends BaseXPersist

class BaseXPersistTest
  extends JUnit4(BaseXPersistTestSpec)

object BaseXPersistTestSpecRunner
  extends ConsoleRunner(BaseXPersistTestSpec)

object BaseXPersistTestSpec extends Specification
{
  "insert" should {
    val timeout = new Duration(500)
    val db = new MockBaseXPersist()

    //Success: 1 created
    "create 1 record" in {
      val collection = "TestDB_Insert"
      val rand = new Random()
      db.drop(collection)

      println("starting single")
      val key = "<contentChannel><String>data2</String></contentChannel>"
      val value = "<String>value goes here" + rand.nextInt + "</String>"
      db.insert(collection, key, value)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(1).eventually(10, timeout)
    }

    //Success: 100 and 1000 created
    "create 100 and 1000 records synchronously" in {
      val collection1 = "StressTestDB_Insert_Small"
      val collection2 = "StressTestDB_Insert_Med"

      db.drop(collection1)
      db.drop(collection2)

      println("starting small")
      for (i <- 1 to 100) {
        val key = "<contentChannel><String>data2</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insert(collection1, key, value)
      }
      println("starting med")
      for (i <- 1 to 1000) {
        val key = "<contentChannel><String>data2</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insert(collection2, key, value)
      }

      println("asserts will wait up to 10 seconds for results")
      db.count(collection1) must be_==(100).eventually(10, timeout)
      db.count(collection2) must be_==(1000).eventually(10, timeout)
    }

    //scales reliably to 250 at a time, ~300+ starts missing writes. without plugin 300/400
    "create 25 and 50 records asynchronously with spawn" in {
      val collection1 = "Async_StressTestDB_Insert_Small"
      val collection2 = "Async_StressTestDB_Insert_Med"
      db.drop(collection1)
      db.drop(collection2)

      println("async starting small")
      for (i <- 1 to 25) {
        spawn {
          val key = "<contentChannel><String>data2</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insert(collection1, key, value)
        }
      }
      println("async starting med")
      for (i <- 1 to 50) {
        spawn {
          val key = "<contentChannel><String>data2</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insert(collection2, key, value)
        }
      }

      db.count(collection1) must be_==(25).eventually(10, timeout)
      db.count(collection2) must be_==(50).eventually(10, timeout)

      //if needing to manually count in gui
      //count(collection('Async_StressTestDB_Insert_Small')/records/record)
      //count(collection('Async_StressTestDB_Insert_Med')/records/record)
    }
  }

  "insert/delete" should {
    val timeout = new Duration(500)
    val db = new MockBaseXPersist()

    val rand = new Random()
    "create and remove 1 record" in {
      val collection = "TestDB_InsertDelete"
      db.drop(collection)

      println("starting single delete")
      val key = "<contentChannel><String>data" + 2 + "</String></contentChannel>"
      val value = "<String>value goes here" + rand.nextInt + "</String>"
      db.insert(collection, key, value)
      db.delete(collection, key)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(0).eventually(10, timeout)
    }

    "create 3 and remove 3 record" in {
      val collection = "TestDB_Multiple_InsertDelete"
      db.drop(collection)

      println("starting multiple create single delete")

      for (i <- 1 to 3) {
        val key = "<contentChannel><String>data" + i + "</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insert(collection, key, value)
      }

      for (i <- 1 to 3) {
        val key = "<contentChannel><String>data" + i + "</String></contentChannel>"
        db.delete(collection, key)
      }

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(0).eventually(10, timeout)
    }


    "create 3 and remove 1 record" in {
      val collection = "TestDB_MultpleSingle_InsertDelete"
      db.drop(collection)

      println("starting multiple create single delete")

      for (i <- 1 to 3) {
        val key = "<contentChannel><String>data" + i + "</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insert(collection, key, value)
      }

      val key = "<contentChannel><String>data2</String></contentChannel>"
      db.delete(collection, key)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(2).eventually(10, timeout)
    }
  }

  "insertUpdate" should {
    val timeout = new Duration(500)
    val db = new MockBaseXPersist()

    "create and update 1 record" in {
      val collection = "TestDB_InsertUpdate"
      val rand = new Random()
      db.drop(collection)

      println("starting create and update 1")
      val key = "<contentChannel><String>data2</String></contentChannel>"
      val value1 = "<String>value goes here" + rand.nextInt + "</String>"
      val value2 = "<String>value goes here 2222" + rand.nextInt + "</String>"
      db.insertUpdate(collection, key, value1)

      db.insertUpdate(collection, key, value2)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(1).eventually(10, timeout)
    }

    "create 2 records" in {
      val collection = "TestDB_InsertUpdateTwice"
      val rand = new Random()
      db.drop(collection)

      println("starting create 1 and update 1")
      val key1 = "<contentChannel><String>data2</String></contentChannel>"
      val key2 = "<contentChannel><String>newData</String></contentChannel>"
      val value1 = "<String>value goes here" + rand.nextInt + "</String>"
      val value2 = "<String>value goes here 2222" + rand.nextInt + "</String>"
      db.insertUpdate(collection, key1, value1)
      db.insertUpdate(collection, key2, value2)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(2).eventually(10, timeout)
    }

    //Success: 100 and 1000 created
    "create 1 and update 100 and 1000 synchronously" in {
      val collection1 = "StressTestDB_InsertUpdate_Small"
      val collection2 = "StressTestDB_InsertUpdate_Med"

      db.drop(collection1)
      db.drop(collection2)

      println("starting small")
      for (i <- 1 to 100) {
        val key = "<contentChannel><String>data2</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insertUpdate(collection1, key, value)
      }
      println("starting med")
      for (i <- 1 to 1000) {
        val key = "<contentChannel><String>data2</String></contentChannel>"
        val value = "<String>value goes here" + i + "</String>"
        db.insertUpdate(collection2, key, value)
      }

      println("asserts will wait up to 10 seconds for results")
      db.count(collection1) must be_==(1).eventually(10, timeout)
      db.count(collection2) must be_==(1).eventually(10, timeout)
    }

    //without the initial timeout the query will race on the exists condition
    //however it is unlikely that we'll hammer insertUpdate with exact same key
    //reasonable at 90 and 200 but some dev machines choke earlier
    "create 1 and update 25 and 50 asynchronously with spawn" in {
      val collection1 = "Async_StressTestDB_InsertUpdate_Small"
      val collection2 = "Async_StressTestDB_InsertUpdate_Med"
      db.drop(collection1)
      db.drop(collection2)

      println("async starting small")
      for (i <- 1 to 25) {
        spawn {
          val key = "<contentChannel><String>data2</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insertUpdate(collection1, key, value)
        }
        if (i == 1) {
          //we need transactions for the first
          Thread.sleep(2000)
        }
      }
      println("async starting med")
      for (i <- 1 to 50) {
        spawn {
          val key = "<contentChannel><String>data2</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insertUpdate(collection2, key, value)
        }
        if (i == 1) {
          Thread.sleep(2000)
        }
      }

      db.count(collection1) must be_==(1).eventually(10, timeout)
      db.count(collection2) must be_==(1).eventually(10, timeout)

      //if needing to manually count in gui
      //count(collection('Async_StressTestDB_Insert_Small')/records/record)
      //count(collection('Async_StressTestDB_Insert_Med')/records/record)
    }

    //reasonable on 50 100 but some dev machines choke earlier
    "create 25 and 50 asynchronously with spawn" in {
      val collection1 = "Async_StressTestDB_Inserting_Small"
      val collection2 = "Async_StressTestDB_Inserting_Med"
      db.drop(collection1)
      db.drop(collection2)

      println("async starting small")
      for (i <- 1 to 25) {
        spawn {
          val key = "<contentChannel><String>data" + i + "</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insertUpdate(collection1, key, value)
        }
        if (i == 1) {
          //we need transactions for the first
          Thread.sleep(2000)
        }
      }
      println("async starting med")
      for (i <- 1 to 50) {
        spawn {
          val key = "<contentChannel><String>data" + i + "</String></contentChannel>"
          val value = "<String>value goes here" + i + "</String>"
          db.insertUpdate(collection2, key, value)
        }
        if (i == 1) {
          //we need transactions for the first
          Thread.sleep(2000)
        }
      }

      db.count(collection1) must be_==(25).eventually(10, timeout)
      db.count(collection2) must be_==(50).eventually(10, timeout)

      //if needing to manually count in gui
      //count(collection('Async_StressTestDB_Insert_Small')/records/record)
      //count(collection('Async_StressTestDB_Insert_Med')/records/record)
    }
  }

  "insertUpdateDelete" should {
    val timeout = new Duration(500)
    val db = new MockBaseXPersist()

    "create and update 1 record" in {
      val collection = "TestDB_InsertUpdateDelete"
      val rand = new Random()
      db.drop(collection)

      println("starting create and update and delete 1")
      val key = "<contentChannel><String>data2</String></contentChannel>"
      val value1 = "<String>value goes here" + rand.nextInt + "</String>"
      val value2 = "<String>value goes here 2222" + rand.nextInt + "</String>"
      db.insertUpdate(collection, key, value1)
      db.insertUpdate(collection, key, value2)
      db.delete(collection, key)

      println("assert will wait up to 5 seconds for results")
      db.count(collection) must be_==(0).eventually(10, timeout)
    }
  }
}



