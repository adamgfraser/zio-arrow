package bench

import java.util.concurrent.TimeUnit

import zio.arrow._
import FileUtils._

object BenchUtils {

  val rand = new scala.util.Random

  /**
   * Bench setup
   */
  val totalWorkers = 5

  // Random seed range for factorial
  val minRange = 8
  val maxRange = 12

  /**
   * Generates a random Int from a specific range
   */
  def fromRange(start: Int, end: Int) = start + rand.nextInt((end - start) + 1)

  /**
   * Simple non-stack safe factorial function
   */
  def factorial(n: Int): Int =
    if (n == 0) return 1
    else return n * factorial(n - 1)

  /**
   * Prepare test file data
   */
  lazy val files = List.tabulate(totalWorkers)(num => ("file" + num.toString, fromRange(minRange, maxRange).toString))

  /**
   * Create test files
   */
  def setup() = {
    newDir("bench")
    files.foreach { f =>
      newFile(f._1)
      wrFile(f._1, f._2)
    }
  }

  /**
   * Remove test files
   */
  def clean() = files.foreach(f => delFile(f._1))

  /**
   * Impure unsafe worker process
   * This performs IO to read the file, gets a value and calculates a `factorial` for that value
   */
  def worker(file: String): Int = {
    // println("Inside a worker")
    // this reads a value from file
    val seed = rdFile(file).fold(0)(data => data.toInt)

    // computes a factorial on the value read
    factorial(seed)
  }

  val workers = files.map(f => worker(f._1))

  /**
   * Calculates the total value from all workers
   */
  def sum(list: List[Int]): Int = list.foldLeft(0) { case (acc, item) => acc + item }

  /**
   * ZIO Arrow worker
   */
  val arrWorker = ZArrow.lift(worker)

  /**
   * Composed Arrow Workers, which comprise a `worker` output for every file from the input list
   */
  val arrWorkers = files.foldLeft(ZArrow.identity[Int]) {
    case (acc, item) => acc >>> ZArrow.lift(_ => worker(item._1))
  }

  def time[R](block: => R): R = {

    val t0        = System.nanoTime()
    val result    = block // call-by-name
    val runtimeNs = System.nanoTime - t0
    val runtimeUs = TimeUnit.MICROSECONDS.convert(runtimeNs, TimeUnit.NANOSECONDS)
    // val runtimeMs = TimeUnit.MILLISECONDS.convert(runtimeNs, TimeUnit.NANOSECONDS)
    // println("Elapsed time: " + runtimeNs + "ns")
    println("Elapsed time: " + runtimeUs + "us")
    // println("Elapsed time: " + runtimeMs + "ms")
    result
  }

  def showTime(runtime: Long): Unit = {
    val runtimeUs = TimeUnit.MICROSECONDS.convert(runtime, TimeUnit.NANOSECONDS)
    println("Total Runstime: " + runtimeUs + "us")
    println()
  }

}
