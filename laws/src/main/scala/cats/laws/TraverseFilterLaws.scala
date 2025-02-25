/*
 * Copyright (c) 2015 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package cats
package laws

import cats.data.Nested
import cats.syntax.all.*
import cats.instances.option.*

trait TraverseFilterLaws[F[_]] extends FunctorFilterLaws[F] {
  implicit override def F: TraverseFilter[F]

  def traverseFilterIdentity[G[_]: Applicative, A](fa: F[A]): IsEq[G[F[A]]] =
    fa.traverseFilter(_.some.pure[G]) <-> fa.pure[G]

  def traverseFilterConsistentWithTraverse[G[_]: Applicative, A](fa: F[A], f: A => G[A]): IsEq[G[F[A]]] =
    fa.traverseFilter(a => f(a).map(_.some)) <-> F.traverse.traverse(fa)(f)

  def traverseFilterComposition[A, B, C, M[_], N[_]](fa: F[A], f: A => M[Option[B]], g: B => N[Option[C]])(implicit
    M: Applicative[M],
    N: Applicative[N]
  ): IsEq[Nested[M, N, F[C]]] = {
    val lhs = Nested[M, N, F[C]](fa.traverseFilter(f).map(_.traverseFilter(g)))
    val rhs: Nested[M, N, F[C]] =
      fa.traverseFilter[Nested[M, N, *], C](a => Nested[M, N, Option[C]](f(a).map(_.traverseFilter(g))))
    lhs <-> rhs
  }

  def filterAConsistentWithTraverseFilter[G[_]: Applicative, A](fa: F[A], f: A => G[Boolean]): IsEq[G[F[A]]] =
    fa.filterA(f) <-> fa.traverseFilter(a => f(a).map(if (_) Some(a) else None))

  def traverseEitherConsistentWithTraverseFilter[G[_], E, A, B](fa: F[A], f: A => G[Option[B]], e: E)(implicit
    G: Monad[G]
  ): IsEq[G[F[B]]] =
    fa.traverseEither(a => f(a).map(_.toRight(e)))((_, _) => Applicative[G].unit) <-> fa.traverseFilter(f)

  def traverseCollectRef[G[_], A, B](fa: F[A], f: PartialFunction[A, G[B]])(implicit
    G: Applicative[G]
  ): IsEq[G[F[B]]] = {
    val lhs = fa.traverseCollect(f)
    val rhs = fa.traverseFilter(a => f.lift(a).sequence)
    lhs <-> rhs
  }
}

object TraverseFilterLaws {
  def apply[F[_]](implicit ev: TraverseFilter[F]): TraverseFilterLaws[F] =
    new TraverseFilterLaws[F] { def F: TraverseFilter[F] = ev }
}
