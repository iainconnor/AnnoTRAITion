package com.iainconnor.annotraition;

public class MasterTrait {
	protected Object traitedObject;

	public MasterTrait ( Object traitedObject ) {
		this.traitedObject = traitedObject;
	}

	public Object getTraitedObject () {
		return traitedObject;
	}
}
