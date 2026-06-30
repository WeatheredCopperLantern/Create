package com.simibubi.create.content.redstone.link.interfaces;

@FunctionalInterface
public interface HexaFunction<P1, P2, P3, P4, P5, P6, R> {
	R apply(P1 var1, P2 var2, P3 var3, P4 var4, P5 var5, P6 var6);
}
