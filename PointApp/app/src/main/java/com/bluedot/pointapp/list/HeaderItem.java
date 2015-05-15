package com.bluedot.pointapp.list;

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
