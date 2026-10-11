package scripts.tests;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import ru.ytkab0bp.slicebeam.utils.LithophaneStlWriter;

public final class LithophaneStlWriterTest {
    public static void main(String[] args) throws Exception {
        File folder = Files.createTempDirectory("orca-lithophane-test").toFile();
        try {
            File panel = new File(folder, "panel.stl");
            float[] relief = {1f, 2f, 3f, 4f};
            LithophaneStlWriter.writePanel(panel, relief, 2, 2, 20f, 30f);
            verifyBinaryStl(panel, 12);
            verifyClosedOutwardMesh(panel);
            Bounds panelBounds = readBounds(panel);
            check(panelBounds.minX == 0f && panelBounds.maxX == 20f, "single panel X bounds");
            check(panelBounds.minY == 0f && panelBounds.maxY == 30f, "single panel Y bounds");
            check(panelBounds.minZ == 0f && panelBounds.maxZ == 4f, "single panel thickness");

            int around = 24, vertical = 12;
            float[] rotationalRelief = new float[around * vertical];
            Arrays.fill(rotationalRelief, 2f);
            for (int shape = 0; shape < 5; shape++) {
                File round = new File(folder, "round-" + shape + ".stl");
                LithophaneStlWriter.writeRotationalLithophane(round, rotationalRelief, around, vertical,
                        80f, 100f, shape, 80f, 3f, shape == 3 ? 8 : 0, true, true);
                verifyBinaryStl(round, around * (4 * (vertical - 1) + 4));
                verifyClosedOutwardMesh(round);
                Bounds roundBounds = readBounds(round);
                check(roundBounds.minZ < 0f && roundBounds.maxZ > 0f,
                        "round lithophane is centered around the rotary axis shape=" + shape);
                check(roundBounds.maxY == 100f, "round lithophane height shape=" + shape);
                expectIOException(() -> LithophaneStlWriter.writeRotationalLithophane(round,
                        rotationalRelief, around, vertical, 80f, 100f, 8, 80f, 0f, 0, true, true));
            }
            File tapered = new File(folder, "round-tapered.stl");
            LithophaneStlWriter.writeRotationalLithophane(tapered, rotationalRelief, around, vertical,
                    80f, 100f, 1, 60f, 0f, 0, true, false);
            verifyBinaryStl(tapered, around * (4 * (vertical - 1) + 2));
            File lightBase = new File(folder, "lithophane-light-base.stl");
            LithophaneStlWriter.writeRoundLightBase(lightBase, 100f, 40f, 7.5f, 6f, 48);
            verifyBinaryStl(lightBase, 48 * 8);
            verifyClosedOutwardMesh(lightBase);
            Bounds lightBaseBounds = readBounds(lightBase);
            check(lightBaseBounds.minX == -50f && lightBaseBounds.maxX == 50f && lightBaseBounds.maxZ == 7.5f,
                    "round light base dimensions");

            File cube = new File(folder, "cube-kit.stl");
            LithophaneStlWriter.writeCubeKit(cube,
                    Arrays.asList(relief, relief, relief, relief), 2, 2, 20f, 30f, 2f, 3f);
            verifyBinaryStl(cube, 60);
            verifyClosedOutwardMesh(cube);
            Bounds cubeBounds = readBounds(cube);
            check(cubeBounds.maxX == 63f, "cube kit non-overlapping panel layout width");
            check(cubeBounds.maxY == 86f, "cube kit non-overlapping panel layout height");
            check(cubeBounds.maxZ == 4f, "cube kit image wall height");
            assertPackedComponentsDoNotOverlap(cube, 12, 5);

            File litoGenFour = new File(folder, "litogen-four-photo.stl");
            LithophaneStlWriter.writeLitoGenCubeKit(litoGenFour,
                    Arrays.asList(relief, relief, relief, relief), null, 2, 2, 0, 0,
                    20f, 30f, 2f, 3f);
            verifyBinaryStl(litoGenFour, 72);
            assertPackedComponentsDoNotOverlap(litoGenFour, 12, 6);
            verifyClosedOutwardMesh(litoGenFour);
            Bounds litoGenFourBounds = readBounds(litoGenFour);
            check(litoGenFourBounds.maxX == 63f && litoGenFourBounds.maxY == 86f,
                    "LitoGEN four-image cube packed bounds");

            File litoGenFive = new File(folder, "litogen-five-photo.stl");
            LithophaneStlWriter.writeLitoGenCubeKit(litoGenFive,
                    Arrays.asList(relief, relief, relief, relief), relief, 2, 2, 2, 2,
                    20f, 30f, 2f, 3f);
            verifyBinaryStl(litoGenFive, 72);
            assertPackedComponentsDoNotOverlap(litoGenFive, 12, 6);
            verifyClosedOutwardMesh(litoGenFive);

            File framed = new File(folder, "framed.stl");
            LithophaneStlWriter.writeFramedPanel(framed, relief, 2, 2, 20f, 30f,
                    5f, 24f, 8f, 3f);
            verifyBinaryStl(framed, 260);
            Bounds frameBounds = readBounds(framed);
            check(frameBounds.minX == -3f && frameBounds.maxX == 23f, "frame bezel width bounds");
            check(frameBounds.minY == 0f && frameBounds.maxY == 57f, "stand and panel bounds");
            check(frameBounds.minZ == 0f && frameBounds.maxZ > 6f, "frame relief bounds");
            check(topNormalZ(panel) > 0f, "relief top normals point outward");

            File tag = new File(folder, "tag.stl");
            float[] flatText = new float[81 * 41];
            Arrays.fill(flatText, 2f);
            LithophaneStlWriter.writeTextTag(tag, flatText, 81, 41, 80f, 40f, 6f);
            int tagFacets = verifyBinaryStl(tag, -1);
            check(tagFacets < (81 - 1) * (41 - 1) * 4 + 2 * ((81 - 1) * 2 + (41 - 1) * 2),
                    "key tag through-hole removes panel faces");
            Bounds tagBounds = readBounds(tag);
            check(tagBounds.minX == 0f && tagBounds.maxX == 80f && tagBounds.maxY == 40f,
                    "key tag dimensions");
            check(!hasHorizontalFaceAt(tag, 75.5f, 20.5f), "key tag keyring hole is open through the base");
            File tagSet = new File(folder, "plant-tag-set.stl");
            int batchFacets = LithophaneStlWriter.writeTextTagBatch(tagSet,
                    Arrays.asList(flatText, flatText, flatText), 81, 41, 80f, 40f, 6f, 6f);
            verifyBinaryStl(tagSet, tagFacets * 3);
            check(batchFacets == tagFacets * 3, "plant marker set facet total");
            Bounds tagSetBounds = readBounds(tagSet);
            check(tagSetBounds.minX == 0f && tagSetBounds.maxX == 166f &&
                    tagSetBounds.minY == 0f && tagSetBounds.maxY == 86f,
                    "three tags arranged as a compact two-column set");
            check(!hasHorizontalFaceAt(tagSet, 75.5f, 20.5f), "first plant marker hole remains open");
            check(!hasHorizontalFaceAt(tagSet, 161.5f, 20.5f), "second plant marker hole remains open");
            check(!hasHorizontalFaceAt(tagSet, 75.5f, 66.5f), "third plant marker hole remains open");
            expectIOException(() -> LithophaneStlWriter.writeTextTagBatch(tagSet,
                    Arrays.asList(flatText, flatText, flatText, flatText, flatText,
                            flatText, flatText, flatText, flatText), 81, 41, 80f, 40f, 6f, 6f));

            File box = new File(folder, "utility-box.stl");
            LithophaneStlWriter.writeUtilityBox(box, 60f, 40f, 30f, 2f, 2f,
                    0.3f, 8f, 6f, true);
            verifyBinaryStl(box, 252);
            Bounds boxBounds = readBounds(box);
            check(boxBounds.minX == 0f && boxBounds.maxX > 130f, "box and lid fit side-by-side");
            check(boxBounds.minY == 0f && boxBounds.maxY > 44f, "box and lid depth bounds");
            check(boxBounds.maxZ == 30f, "box body is taller than its lid");
            check(!hasHorizontalFaceAt(box, 30f, 20f), "box cable pass-through is open in the floor");
            check(maxHorizontalFaceZAt(box, 10f, 10f) == 2f, "box remains hollow above its floor");
            File packedBox = new File(folder, "packed-box.stl");
            LithophaneStlWriter.writeUtilityBox(packedBox, 140f, 60f, 60f, 2f, 2f,
                    0.3f, 8f, 0f, true);
            verifyBinaryStl(packedBox, 64);
            Bounds packedBounds = readBounds(packedBox);
            check(packedBounds.maxX < 150f && packedBounds.maxY < 135f,
                    "large lid automatically stacks beside the box on a compact plate");
            expectIOException(() -> LithophaneStlWriter.writeUtilityBox(box, 7f, 10f, 10f,
                    2f, 2f, 0.3f, 8f, 0f, true));

            File clip = new File(folder, "cable-clip.stl");
            LithophaneStlWriter.writeCableClip(clip, 6f, 0.35f, 2.4f,
                    12f, 4.8f, true, 18f, 4.2f);
            verifyBinaryStl(clip, 780);
            Bounds clipBounds = readBounds(clip);
            check(Math.abs(clipBounds.minX + 8.4f) < 0.001f && Math.abs(clipBounds.maxX - 8.4f) < 0.001f,
                    "cable clip mounting tab width: " + clipBounds.minX + ".." + clipBounds.maxX);
            check(Math.abs(clipBounds.minY + 22.43f) < 0.01f && clipBounds.maxY > 5.2f,
                    "cable clip and mounting tab bounds: " + clipBounds.minY + ".." + clipBounds.maxY);
            check(clipBounds.minZ == 0f && clipBounds.maxZ == 12f, "cable clip print orientation and width");
            check(!hasHorizontalFaceAt(clip, 0f, 0f), "cable opening remains hollow");
            check(!hasHorizontalFaceAt(clip, 0f, 5.2f), "C clip mouth remains open");
            check(!hasHorizontalFaceAt(clip, 0f, -13.43f), "mount screw hole remains open");
            File unmountedClip = new File(folder, "cable-clip-no-tab.stl");
            LithophaneStlWriter.writeCableClip(unmountedClip, 4f, 0f, 1.6f,
                    8f, 2.4f, false, 0f, 0f);
            verifyBinaryStl(unmountedClip, 580);
            expectIOException(() -> LithophaneStlWriter.writeCableClip(clip, 6f, 0.35f, 2.4f,
                    12f, 6f, true, 18f, 4.2f));

            File grommet = new File(folder, "cable-grommet.stl");
            LithophaneStlWriter.writeCableGrommet(grommet, 12f, 0.4f, 2f,
                    50f, 3f, 20f, 4.2f);
            verifyBinaryStl(grommet, 888);
            Bounds grommetBounds = readBounds(grommet);
            check(grommetBounds.minX == -37f && grommetBounds.maxX == 37f,
                    "grommet flange and mounting ears fit expected width: " + grommetBounds.minX + ".." + grommetBounds.maxX);
            check(grommetBounds.minY == -25f && grommetBounds.maxY == 25f,
                    "grommet flange bounds");
            check(grommetBounds.minZ == 0f && grommetBounds.maxZ == 23f,
                    "grommet flange and sleeve height");
            check(!hasHorizontalFaceAt(grommet, 0f, 0f), "grommet cable passage is open");
            check(!hasHorizontalFaceAt(grommet, -29f, 0f), "left mounting screw hole is open");
            check(!hasHorizontalFaceAt(grommet, 29f, 0f), "right mounting screw hole is open");
            expectIOException(() -> LithophaneStlWriter.writeCableGrommet(grommet,
                    30f, 1f, 5f, 40f, 3f, 20f, 4f));

            File wallHolder = new File(folder, "wall-tool-holder.stl");
            LithophaneStlWriter.writeWallToolHolder(wallHolder, 16f, 0.4f, 4, 3f, 4.2f);
            verifyBinaryStl(wallHolder, 2000);
            Bounds holderBounds = readBounds(wallHolder);
            check(Math.abs(holderBounds.minX + 69.6f) < 0.001f &&
                    Math.abs(holderBounds.maxX - 69.6f) < 0.001f, "wall holder row and mounts width");
            check(Math.abs(holderBounds.minY + 14.4f) < 0.001f &&
                    Math.abs(holderBounds.maxY - 14.4f) < 0.001f, "wall holder depth");
            check(holderBounds.minZ == 0f && Math.abs(holderBounds.maxZ - 15.8f) < 0.001f,
                    "wall holder guide sleeves print upright");
            for (float x : new float[]{-43.2f, -14.4f, 14.4f, 43.2f})
                check(!hasHorizontalFaceAt(wallHolder, x, 0f), "tool opening is through at x=" + x);
            check(!hasHorizontalFaceAt(wallHolder, -61.6f, 0f), "left wall screw hole is open");
            check(!hasHorizontalFaceAt(wallHolder, 61.6f, 0f), "right wall screw hole is open");
            expectIOException(() -> LithophaneStlWriter.writeWallToolHolder(
                    wallHolder, 16f, 0.4f, 1, 3f, 4.2f));

            File curved = new File(folder, "curved-lithophane.stl");
            float[] curvedGrid = new float[32 * 20];
            java.util.Arrays.fill(curvedGrid, 3.2f);
            LithophaneStlWriter.writeCurvedPanel(curved, curvedGrid, 32, 20, 80f, 60f, 55f);
            verifyBinaryStl(curved, 2556);
            verifyClosedOutwardMesh(curved);
            Bounds curvedBounds = readBounds(curved);
            check(curvedBounds.maxX - curvedBounds.minX > 60f,
                    "curved lithophane spans the requested image width");
            check(curvedBounds.maxZ - curvedBounds.minZ > 2f,
                    "curved lithophane has non-flat depth");
            expectIOException(() -> LithophaneStlWriter.writeCurvedPanel(
                    curved, curvedGrid, 32, 20, 80f, 60f, 10f));

            File planter = new File(folder, "planter-and-saucer.stl");
            LithophaneStlWriter.writePlanterPot(planter, 70f, 80f, 80f, 2.4f,
                    3f, 5f, true, 3f, 2f, 8f, 2f);
            verifyBinaryStl(planter, 1280);
            Bounds planterBounds = readBounds(planter);
            check(planterBounds.minX == -40f && planterBounds.maxX == 121f,
                    "planter and saucer laid side-by-side");
            check(planterBounds.minY == -40f && planterBounds.maxY == 40f,
                    "planter and saucer footprint depth");
            check(planterBounds.minZ == 0f && planterBounds.maxZ == 80f,
                    "planter height and print orientation");
            check(!hasHorizontalFaceAt(planter, 0f, 0f), "planter drain opening passes through base");
            check(maxHorizontalFaceZAt(planter, 0f, 20f) == 3f,
                    "planter interior stays hollow above floor");
            check(maxHorizontalFaceZAt(planter, 81f, 0f) == 2f,
                    "saucer is a hollow basin with a solid floor");
            File potOnly = new File(folder, "planter-only.stl");
            LithophaneStlWriter.writePlanterPot(potOnly, 70f, 80f, 80f, 2.4f,
                    3f, 5f, false, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
            verifyBinaryStl(potOnly, 768);
            expectIOException(() -> LithophaneStlWriter.writePlanterPot(planter,
                    70f, 80f, 80f, 2.4f, 3f, 50f, true, 3f, 2f, 8f, 2f));

            File phoneStand = new File(folder, "phone-stand.stl");
            LithophaneStlWriter.writePhoneStand(phoneStand, 84f, 10f, 20f,
                    110f, 100f, 6f, 16f, 16f, 22f);
            verifyBinaryStl(phoneStand, 72);
            Bounds standBounds = readBounds(phoneStand);
            check(standBounds.minX == 0f && standBounds.maxX == 84f &&
                    standBounds.minY == 0f && standBounds.maxY == 110f,
                    "phone stand footprint");
            check(standBounds.minZ == 0f && standBounds.maxZ == 100f,
                    "phone stand back support height");
            check(!hasHorizontalFaceAt(phoneStand, 42f, 10f), "front cable route remains open");
            check(hasHorizontalFaceAt(phoneStand, 42f, 30f), "rear base bridge connects both stand feet");
            expectIOException(() -> LithophaneStlWriter.writePhoneStand(phoneStand,
                    84f, 10f, 35f, 65f, 140f, 8f, 16f, 16f, 22f));

            File powerStrip = new File(folder, "power-strip-tray.stl");
            LithophaneStlWriter.writePowerStripTray(powerStrip, 180f, 45f, 30f,
                    2f, 3f, 12f, 14f, 4.2f);
            verifyBinaryStl(powerStrip, 872);
            Bounds stripBounds = readBounds(powerStrip);
            check(stripBounds.minX == -14f && stripBounds.maxX == 70f &&
                    stripBounds.minY == 0f && stripBounds.maxY == 184f,
                    "power strip tray and mounting footprint");
            check(stripBounds.minZ == 0f && stripBounds.maxZ == 15f,
                    "power strip end stop height");
            for (float y : new float[]{10f,174f}) {
                check(!hasHorizontalFaceAt(powerStrip,-6f,y), "left power tray screw hole stays open");
                check(!hasHorizontalFaceAt(powerStrip,62f,y), "right power tray screw hole stays open");
            }
            check(maxHorizontalFaceZAt(powerStrip,28f,182.25f)==3f,
                    "rear cable notch remains open above the tray floor");
            check(maxHorizontalFaceZAt(powerStrip,12f,182.25f)==15f,
                    "rear end stops remain on both sides of cable notch");
            expectIOException(() -> LithophaneStlWriter.writePowerStripTray(powerStrip,
                    180f,45f,30f,2f,3f,12f,40f,4.2f));

            expectIOException(() -> LithophaneStlWriter.writeCubeKit(cube,
                    Arrays.asList(relief, relief, relief), 2, 2, 20f, 30f, 2f, 3f));
            expectIOException(() -> LithophaneStlWriter.writePanel(panel,
                    new float[]{1f, Float.NaN, 2f, 3f}, 2, 2, 20f, 30f));
            System.out.println("LithophaneStlWriterTest passed: flat, curved, spherical/vase/bell/pumpkin/lampshade lithophanes, LED bases, tags, boxes, clips/grommets, planters, bases, facet counts and invalid inputs.");
        } finally {
            for (File file : folder.listFiles()) file.delete();
            folder.delete();
        }
    }

    private static int verifyBinaryStl(File file, int expectedFacets) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int facets = ByteBuffer.wrap(data, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (expectedFacets >= 0) check(facets == expectedFacets, "STL facet count");
        check(data.length == 84L + facets * 50L, "STL binary size matches facet count");
        for (int offset = 84; offset < data.length; offset += 50) {
            for (int index = 0; index < 12; index++) {
                float value = ByteBuffer.wrap(data, offset + index * 4, 4)
                        .order(ByteOrder.LITTLE_ENDIAN).getFloat();
                check(Float.isFinite(value), "STL contains finite vertex data");
            }
        }
        return facets;
    }

    private static Bounds readBounds(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        Bounds bounds = new Bounds();
        for (int offset = 84; offset < data.length; offset += 50) {
            for (int vertex = 0; vertex < 3; vertex++) {
                int start = offset + 12 + vertex * 12;
                float x = readFloat(data, start), y = readFloat(data, start + 4), z = readFloat(data, start + 8);
                bounds.minX = Math.min(bounds.minX, x); bounds.maxX = Math.max(bounds.maxX, x);
                bounds.minY = Math.min(bounds.minY, y); bounds.maxY = Math.max(bounds.maxY, y);
                bounds.minZ = Math.min(bounds.minZ, z); bounds.maxZ = Math.max(bounds.maxZ, z);
            }
        }
        return bounds;
    }

    private static void assertPackedComponentsDoNotOverlap(File file, int facetsPerPart,
                                                             int parts) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        Bounds[] bounds = new Bounds[parts];
        for (int part = 0; part < parts; part++) {
            Bounds b = new Bounds();
            int from = 84 + part * facetsPerPart * 50;
            int to = from + facetsPerPart * 50;
            for (int offset = from; offset < to; offset += 50) for (int vertex = 0; vertex < 3; vertex++) {
                int start = offset + 12 + vertex * 12;
                float x=readFloat(data,start), y=readFloat(data,start+4), z=readFloat(data,start+8);
                b.minX=Math.min(b.minX,x); b.maxX=Math.max(b.maxX,x);
                b.minY=Math.min(b.minY,y); b.maxY=Math.max(b.maxY,y);
                b.minZ=Math.min(b.minZ,z); b.maxZ=Math.max(b.maxZ,z);
            }
            bounds[part]=b;
        }
        for(int i=0;i<parts;i++)for(int j=i+1;j<parts;j++) {
            Bounds a=bounds[i],b=bounds[j];
            boolean overlap=a.maxX>b.minX+1e-4f&&b.maxX>a.minX+1e-4f&&
                    a.maxY>b.minY+1e-4f&&b.maxY>a.minY+1e-4f&&
                    a.maxZ>b.minZ+1e-4f&&b.maxZ>a.minZ+1e-4f;
            check(!overlap,"packed parts overlap: "+i+" and "+j);
        }
    }

    private static void verifyClosedOutwardMesh(File file) throws IOException {
        byte[] data=Files.readAllBytes(file.toPath());
        int facets=ByteBuffer.wrap(data,80,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        java.util.Map<String,Integer> edges=new java.util.HashMap<>();
        double signedVolume=0;
        for(int i=0;i<facets;i++){
            int offset=84+i*50;
            float nx=readFloat(data,offset),ny=readFloat(data,offset+4),nz=readFloat(data,offset+8);
            float[] v=new float[9];for(int j=0;j<9;j++)v[j]=readFloat(data,offset+12+j*4);
            float ux=v[3]-v[0],uy=v[4]-v[1],uz=v[5]-v[2],vx=v[6]-v[0],vy=v[7]-v[1],vz=v[8]-v[2];
            float cx=uy*vz-uz*vy,cy=uz*vx-ux*vz,cz=ux*vy-uy*vx;
            check(cx*cx+cy*cy+cz*cz>1e-12f,"LitoGEN contains a zero-area triangle");
            float normalLength=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            check(normalLength>.99f&&normalLength<1.01f,"LitoGEN facet has an invalid normal");
            check(cx*nx+cy*ny+cz*nz>0,"LitoGEN facet normal opposes its triangle winding");
            signedVolume+=(v[0]*(v[4]*v[8]-v[5]*v[7])+v[1]*(v[5]*v[6]-v[3]*v[8])+v[2]*(v[3]*v[7]-v[4]*v[6]))/6.0;
            String a=vertex(v,0),b=vertex(v,3),c=vertex(v,6);edge(edges,a,b);edge(edges,b,c);edge(edges,c,a);
        }
        check(signedVolume>0,"LitoGEN shell winding is inward");
        for(java.util.Map.Entry<String,Integer> edge:edges.entrySet())
            check(edge.getValue()==2,"Lithophane mesh contains an open or non-manifold edge in " +
                    file.getName() + " (incidences=" + edge.getValue() + ", edge=" + edge.getKey() + ")");
    }

    private static String vertex(float[]v,int i){return Float.floatToIntBits(v[i])+":"+Float.floatToIntBits(v[i+1])+":"+Float.floatToIntBits(v[i+2]);}
    private static void edge(java.util.Map<String,Integer> map,String a,String b){String k=a.compareTo(b)<0?a+"|"+b:b+"|"+a;map.put(k,map.getOrDefault(k,0)+1);}

    private static float readFloat(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    private static float topNormalZ(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        return readFloat(data, 84 + 8);
    }

    private static boolean hasHorizontalFaceAt(File file, float px, float py) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int facets = ByteBuffer.wrap(data, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        for (int i = 0, offset = 84; i < facets; i++, offset += 50) {
            float nz = readFloat(data, offset + 8);
            if (Math.abs(nz) < 0.99f) continue;
            float ax=readFloat(data,offset+12), ay=readFloat(data,offset+16);
            float bx=readFloat(data,offset+24), by=readFloat(data,offset+28);
            float cx=readFloat(data,offset+36), cy=readFloat(data,offset+40);
            float area = (bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
            float w0=((bx-px)*(cy-py)-(by-py)*(cx-px))/area;
            float w1=((cx-px)*(ay-py)-(cy-py)*(ax-px))/area;
            float w2=1f-w0-w1;
            if (w0 >= -1e-5f && w1 >= -1e-5f && w2 >= -1e-5f) return true;
        }
        return false;
    }

    private static float maxHorizontalFaceZAt(File file, float px, float py) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int facets = ByteBuffer.wrap(data, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        float max = Float.NEGATIVE_INFINITY;
        for (int i=0,offset=84;i<facets;i++,offset+=50) {
            if (Math.abs(readFloat(data,offset+8)) < 0.99f) continue;
            float ax=readFloat(data,offset+12),ay=readFloat(data,offset+16);
            float bx=readFloat(data,offset+24),by=readFloat(data,offset+28);
            float cx=readFloat(data,offset+36),cy=readFloat(data,offset+40);
            float area=(bx-ax)*(cy-ay)-(by-ay)*(cx-ax);
            float w0=((bx-px)*(cy-py)-(by-py)*(cx-px))/area;
            float w1=((cx-px)*(ay-py)-(cy-py)*(ax-px))/area;
            float w2=1f-w0-w1;
            if (w0>=-1e-5f&&w1>=-1e-5f&&w2>=-1e-5f)
                max=Math.max(max,Math.max(readFloat(data,offset+20),Math.max(readFloat(data,offset+32),readFloat(data,offset+44))));
        }
        return max;
    }

    private static boolean hasSurfaceAt(File file, float px, float py, float pz) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int facets = ByteBuffer.wrap(data, 80, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        for (int i = 0, offset = 84; i < facets; i++, offset += 50) {
            float ax=readFloat(data,offset+12), ay=readFloat(data,offset+16), az=readFloat(data,offset+20);
            float bx=readFloat(data,offset+24), by=readFloat(data,offset+28), bz=readFloat(data,offset+32);
            float cx=readFloat(data,offset+36), cy=readFloat(data,offset+40), cz=readFloat(data,offset+44);
            float ux=bx-ax, uy=by-ay, uz=bz-az, vx=cx-ax, vy=cy-ay, vz=cz-az;
            float nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
            float norm=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if (norm < 1e-8f || Math.abs((px-ax)*nx+(py-ay)*ny+(pz-az)*nz)/norm > 1e-3f) continue;
            float wx=px-ax, wy=py-ay, wz=pz-az;
            float uu=ux*ux+uy*uy+uz*uz, uv=ux*vx+uy*vy+uz*vz, vv=vx*vx+vy*vy+vz*vz;
            float wu=wx*ux+wy*uy+wz*uz, wv=wx*vx+wy*vy+wz*vz;
            float denominator=uu*vv-uv*uv;
            if (Math.abs(denominator) < 1e-8f) continue;
            float b=(wu*vv-wv*uv)/denominator, c=(wv*uu-wu*uv)/denominator, a=1f-b-c;
            if (a >= -1e-5f && b >= -1e-5f && c >= -1e-5f) return true;
        }
        return false;
    }

    private static void expectIOException(ThrowingRunnable action) throws Exception {
        try { action.run(); } catch (IOException expected) { return; }
        throw new AssertionError("Invalid mesh input must be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private interface ThrowingRunnable { void run() throws Exception; }

    private static final class Bounds {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
    }
}
