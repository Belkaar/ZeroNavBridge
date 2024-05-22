package team.burkart.zero

class Observable<T>(private var value : T) {
	private val observers : MutableList<(value: T, oldValue: T)->Unit> = ArrayList()

	fun get() : T {return value;}

	fun set(newValue: T) {
		observers.forEach { observer -> observer.invoke(newValue, value); }
		value = newValue
	}

	fun observe (callback: (value: T, oldValue: T)->Unit) {
		observers.add(callback)
		callback.invoke(value, value)
	}
}