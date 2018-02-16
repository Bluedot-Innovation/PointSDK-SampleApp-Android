package com.bluedot.pointapp.list;

/*
 * @author Bluedot Innovation
 * Copyright (c) 2018 Bluedot Innovation. All rights reserved.
 */
public class HeaderItem {

	private String name = "";
    private String id = "";

	public HeaderItem(String name, String id) {
		this.name = name;
        this.id = id;
	}

	@Override
	public String toString() {
		return name;
	}

    public String getId() {
        return id;
    }
}
