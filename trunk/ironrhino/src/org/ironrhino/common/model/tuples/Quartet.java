package org.ironrhino.common.model.tuples;

import java.io.Serializable;

public class Quartet<A, B, C, D> implements Serializable {

	private A a;

	private B b;

	private C c;

	private D d;

	public Quartet() {

	}

	public Quartet(A a, B b, C c, D d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}

	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}

	public D getD() {
		return d;
	}

	public void setD(D d) {
		this.d = d;
	}

}