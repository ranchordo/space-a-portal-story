package game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;

import objects.Thing;
import lepton.engine.rendering.lighting.Lighting;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;

public class Chamber implements Serializable {
	private static final long serialVersionUID = 1362178040611645003L;
	public ArrayList<Thing> stuff=new ArrayList<Thing>(); //DO NOT PUT PLAYERS IN HERE
	public static void handleIDAssignment(Thing thing) {
		thing.activates_ser=new HashSet<Integer>();
		for(Thing conn : thing.activates) {
			thing.activates_ser.add(conn.id);
		}
		if(thing.getIdFieldAssoc()!=null) {
			Field[] fields=thing.getClass().getDeclaredFields();
			thing.getIdFieldAssoc().clear();
			try {
				for(Field f : fields) {
					if(f.isAnnotationPresent(Thing.SerializeByID.class) && f.getType()==Thing.class) {
						if(f.get(thing)==null) {
							thing.getIdFieldAssoc().put(f.getName(),-1);
						} else {
							thing.getIdFieldAssoc().put(f.getName(),((Thing)f.get(thing)).id);
						}
						Logger.log(0,"Placing numeric ID value of SerializeByID-annotated Thing field with name "+thing.type+"."+f.getName());
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException e) {
				Logger.log(4,e.toString(),e);
			}
		} else {
			Logger.log(0,"Cancelling "+thing.type+"'s field scan.");
		}
	}
	public static void handleIDGet(Thing thing, Chamber ch) {
		thing.activates=new HashSet<Thing>();
		for(int i : thing.activates_ser) {
			thing.activates.add(ch.id(i));
		}
		thing.activates_ser=new HashSet<Integer>();
		if(thing.getIdFieldAssoc()!=null) {
			Field[] fields=thing.getClass().getDeclaredFields();
			try {
				for(Field f : fields) {
					if(f.isAnnotationPresent(Thing.SerializeByID.class) && f.getType()==Thing.class) {
						if(!thing.getIdFieldAssoc().containsKey(f.getName())) {
							Logger.log(4,"IdFieldAssociation mappings do not contain serialized field name of SerializeByID-annotated Thing field: "+f.getName());
						} else {
							int id=thing.getIdFieldAssoc().get(f.getName());
							if(id==-1) {
								f.set(thing,null);
							} else {
								f.set(thing,ch.id(id));
							}
						}
					}
				}
			} catch (IllegalAccessException | IllegalArgumentException e) {
				Logger.log(4,e.toString(),e);
			}
		}
	}
	public void assignIDs() {
		int i=0;
		for(Thing thing : stuff) {
			thing.id=i;
			i++;
		}
		for(Thing thing : stuff) {
			handleIDAssignment(thing);
		}
	}
	public Thing id(int i) {
		for(Thing thing : stuff) {
			if(thing.id==i) {
				return thing;
			}
		}
		Logger.log(4,"Id "+i+" is not in the same testchamber as its user.");
		return null;
	}
	public void fromIDs() {
		for(Thing thing : stuff) {
			handleIDGet(thing,this);
		}
	}
	public void output(String fname) {
		String complete_fname=null;
		try {
			complete_fname=LeptonUtil.getExternalPath()+"/assets/chambers/"+fname+".chmb";
			File creation=new File(complete_fname);
			creation.createNewFile();
			FileOutputStream scr_output=new FileOutputStream(complete_fname);
			ObjectOutputStream scr_out=new ObjectOutputStream(scr_output);
			assignIDs();
			scr_out.writeObject(this);
			scr_out.close();
			scr_output.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			Logger.log(3,"Erroneous filename is: "+complete_fname);
			e.printStackTrace();
			System.exit(1);
		}
	}
	public Thing add(Thing toAdd) {
		this.stuff.add(toAdd);
		return toAdd;
	}
	public static Chamber input(String fname) {
		Lighting.clear();
		String complete_fname=LeptonUtil.getExternalPath()+"/assets/chambers/"+fname+".chmb";
		InputStream inStream=null;
		try {
			inStream=new FileInputStream(complete_fname);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			ObjectInputStream scr_in=new ObjectInputStream(inStream);
			Chamber out=(Chamber)scr_in.readObject();
			out.fromIDs();
			for(Thing t : out.stuff) {
				t.onSerialization();
			}
			return out;
		} catch (IOException e) {
			e.printStackTrace();
			Logger.log(4,"IOException while reading "+complete_fname,e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Logger.log(4,"Invalid chmb format",e);
		}
		return null;
	}
}
