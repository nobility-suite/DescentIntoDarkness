package com.gmail.sharpcastle33.did;

public class Pair<A, B> {
	public final A left;
	public final B right;

	public static <A, B> Pair<A, B> of(A left, B right) {
		return new Pair<A, B>(left, right);
	}

	public Pair(A a, B b) {
		left = a;
		right = b;
	}

	public A getLeft() {
		return left;
	}

	public B getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "(" + left + ", " + right + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> p = (Pair<?, ?>) o;
			return left.equals(p.left) && right.equals(p.right);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return left.hashCode() ^ right.hashCode();
	}
}
