package org.osc.core.server.installer.impl;

import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

final class Locking {

	public static void withLock(Lock lock, Runnable cmd) {
		lock.lock();
		try {
			cmd.run();
		} finally {
			lock.unlock();
		}
	}

	public static <R> R withLock(Lock lock, Supplier<R> fun) {
		lock.lock();
		try {
			return fun.get();
		} finally {
			lock.unlock();
		}
	}

	public static <T, R> R withLock(Lock lock, Function<T, R> fun, T in) {
		lock.lock();
		try {
			return fun.apply(in);
		} finally {
			lock.unlock();
		}
	}

}
