package adventures.task

import monix.eval.Task

import scala.concurrent.duration.FiniteDuration

/**
  * If a result 'A' is available synchronously, then that same result asynchronously
  * could be represented as a 'Task[A]'
  */
object TaskAdventures {

  /**
    * 1.  Create a task which returns 43
    *
    * See https://monix.io/docs/2x/eval/task.html#simple-builders for ways to construct Tasks
    */

  // Task.now lifts an already known value in the Task context, the equivalent of Future.successful or of Applicative.pure:
  //
  // val task = Task.now { println("Effect"); "Hello!" }
  //=> Effect
  // task: monix.eval.Task[String] = Delay(Now(Hello!))

  def immediatelyExecutingTask(): Task[Int] = {
    Task.now(43)
  }

  /**
    * 2.	Create a Task which when executed logs “hello world” (using `logger`)
    */

  // Task.eval is the equivalent of Function0, taking a function that will always be evaluated on runAsync,
  // possibly on the same thread (depending on the chosen execution model):
  //
  // val task = Task.eval { println("Effect"); "Hello!" }
  // task: monix.eval.Task[String] = Delay(Always(<function0>))
  //
  // The evaluation (and thus all contained side effects)
  // gets triggered on each runAsync:
  //
  // task.runAsync.foreach(println)
  //=> Effect
  //=> Hello!

  def helloWorld(logger: String => Unit): Task[Unit] = {
    Task.eval(logger("hello world"))
  }

  /**
    * 3.	Create a Task which always fails.
    *
    * See https://monix.io/docs/2x/eval/task.html#taskraiseerror
    */

  // Task.raiseError can lift errors in the monadic context of Task

  def alwaysFailingTask(): Task[Unit] = {
    Task.raiseError(new Exception("Die"))
  }

  /**
    * 4.	There is 1 remote service which will return you a task that provides the current temperature in celsius.
    *
    */
  def getCurrentTempInF(currentTemp: () => Task[Int]): Task[Int] = {
    def cToF(c: Int) = c * 9 / 5 + 32

    currentTemp().map(cToF)
  }

  /**
    * 5.	There is 1 remote service which will return you a task that provides the current temperature in celsius.
    * The conversion is complex so we have decided to refactor it out to another remote microservice.
    * Make use of both of these services to return the current temperature in fahrenheit.
    */
  def getCurrentTempInFAgain(currentTemp: () => Task[Int], converter: Int => Task[Int]): Task[Int] = {
    currentTemp().flatMap(converter)
  }

  /**
    * 6. Computing the complexity of a string is a very expensive op.  Implement this function so that complexity
    * of the calculation will be done in parallel.  Sum the returned results to
    * provide the overall complexity for all Strings.  (try using operations from monix)
    *
    * Also, check out the spec for this to see how the Monix TestScheduler can be used to simulate the passage of time
    * in tests.
    */

  // Task.gather, also known as Task.zipList, is the nondeterministic version of Task.sequence.
  // It also takes a Seq[Task[A]] and returns a Task[Seq[A]],
  // thus transforming any sequence of tasks into a task with a sequence of ordered results.
  // But the effects are not ordered, meaning that there’s potential for parallel execution.

  def calculateStringComplexityInParallel(strings: List[String], complexity: String => Task[Int]): Task[Int] = {
    //    val tasks: Seq[Task[Int]] = strings.map(complexity)

    Task.parTraverse(strings)(complexity).map(_.sum)
  }

  /**
    * 6.b. As above, but try to implement the parallel processing using the monix Applicative instance for Task
    * and the cats `sequence` function. (if you haven't heard of cats / sequence skip this - even if you have consider
    * this as optional).
    *
    * The following imports will help.
    * import cats.implicits._
    * implicit def parTaskApplicative: Applicative[eval.Task.Par] = Task.catsParallel.applicative
    *
    * Note that you will also need to convert from Task to Task.Par for the cats sequence operator to execute the tasks
    * in parallel.
    */
  def calculateStringComplexityInParallelAgain(strings: List[String], complexity: String => Task[Int]): Task[Int] = {
    import cats.implicits._
    //    implicit def parTaskApplicative: Applicative[eval.Task.Par] = Task.catsParallel.applicative
    //
    //    val task: List[Task.Par[Int]] = strings.map(complexity).map(Task.Par.apply)
    //
    //    val parTask = task.sequence[Task.Par, Int].map(_.sum)
    //    Task.Par.unwrap(parTask)

    strings.parTraverse(complexity).map(_.sum)
  }

  /**
    * 7.	Write a function which given a Task, will reattempt that task after a specified delay for a maximum number of
    * attempts if the supplied Task fails.
    */

  // Task.onErrorHandleWith is an operation that takes a function mapping possible exceptions to a desired fallback outcome

  def retryOnFailure[T](task: Task[T], maxRetries: Int, delay: FiniteDuration): Task[T] = {
    def retry(remainingAttempts: Int): Task[T] = {
      task.onErrorHandleWith { error =>
        if (remainingAttempts > 0)
          retry(remainingAttempts - 1).delayExecution(delay)
        else
          Task.raiseError(error)
      }
    }

    retry(maxRetries)
  }

}
