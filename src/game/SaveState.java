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
import java.util.Arrays;
import java.util.List;

import lepton.engine.rendering.lighting.Lighting;
import lepton.util.LeptonUtil;
import lepton.util.advancedLogger.Logger;
import objects.Player;
import objects.PortalPair;
import objects.Thing;

public class SaveState implements Serializable {
	private static final long serialVersionUID = -8268014585329721063L;
	public static final List<String> excludes=Arrays.asList(new String[] {"Player", "Portal_pair"});
	public Chamber currentEnvironment;
	public Player currentPlayer;
	public PortalPair portalPair;
	public void output() {
		Logger.log(0,"Saving savestate to /save.state");
		try {
			String complete_fname=LeptonUtil.getExternalPath()+"/save.state";
			File creation=new File(complete_fname);
			creation.createNewFile();
			FileOutputStream scr_output=new FileOutputStream(complete_fname);
			ObjectOutputStream scr_out=new ObjectOutputStream(scr_output);
			currentEnvironment.assignIDs();
			Chamber.handleIDAssignment(currentPlayer);
			Chamber.handleIDAssignment(portalPair);
			scr_out.writeObject(this);
			scr_out.close();
			scr_output.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	public static SaveState input() {
		Lighting.clear();
		String complete_fname=LeptonUtil.getExternalPath()+"/save.state";
		InputStream inStream=null;
		try {
			inStream=new FileInputStream(complete_fname);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			ObjectInputStream scr_in=new ObjectInputStream(inStream);
			SaveState out=(SaveState)scr_in.readObject();
			out.currentEnvironment.fromIDs();
			Chamber.handleIDGet(out.currentPlayer,out.currentEnvironment);
			Chamber.handleIDGet(out.portalPair,out.currentEnvironment);
			for(Thing t : out.currentEnvironment.stuff) {
				t.onSerialization();
			}
			out.currentPlayer.onSerialization();
			out.portalPair.onSerialization();
			out.currentPlayer.handleNewSSPP(out.portalPair);
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
	public void create() {
		Logger.log(0,"Copying data to savestate");
		currentEnvironment=new Chamber();
		for(Thing thing : Main.things) {
			if(!excludes.contains(thing.type)) {
				thing.doSaveState();
				currentEnvironment.add(thing);
			}
		}
		PlayerInitializer.player.doSaveState();
		PlayerInitializer.player.portalPair.doSaveState();
		currentPlayer=PlayerInitializer.player;
		portalPair=PlayerInitializer.player.portalPair;
	}
}
