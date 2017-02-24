package org.osc.core.server.installer.impl;

import java.util.concurrent.Callable;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

class Job<T> implements Runnable {

	private final Deferred<T> deferred = new Deferred<>();
	private final Callable<T> call;

	Job(Callable<T> call) {
		this.call = call;
	}

	@Override
	public void run() {
		// Avoid repeating the work
		if (this.deferred.getPromise().isDone()) {
            return;
        }

		try {
			T result = this.call.call();
			this.deferred.resolve(result);
		} catch (Exception e) {
			this.deferred.fail(e);
		}
	}

	public Promise<T> getPromise() {
		return this.deferred.getPromise();
	}


}
