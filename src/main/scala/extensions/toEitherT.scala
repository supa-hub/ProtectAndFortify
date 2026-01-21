package extensions

import cats.data.EitherT


extension [F[_], A, B](x: F[Either[A, B]])
  def toEitherT: EitherT[F, A, B] = EitherT(x)
