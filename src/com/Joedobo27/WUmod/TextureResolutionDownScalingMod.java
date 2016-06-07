package com.Joedobo27.WUmod;


import com.wurmonline.client.options.Options;
import com.wurmonline.client.resources.textures.PreProcessedTextureData;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class TextureResolutionDownScalingMod implements WurmClientMod, Initable {

    private static ClassPool pool;

    @Override
    public void init() {
        pool = HookManager.getInstance().getClassPool();
        try {
            doPreProcessedTextureDataHook();
        } catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private static void doPreProcessedTextureDataHook() throws NotFoundException, CannotCompileException {
        CtMethod cmCall = pool.get("com.wurmonline.client.resources.textures.HttpTextureBuilder").getMethod("call",
                "()Lcom/wurmonline/client/resources/textures/PreProcessedTextureData;");
        cmCall.instrument(new ExprEditor(){
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("preprocessImage", methodCall.getMethodName())){
                    methodCall.replace("$_ = com.Joedobo27.WUmod.TextureResolutionDownScalingMod.preprocessedImageHook($1, 8)");
                }
            }
        });

        CtMethod cmLoadTexture = pool.get("com.wurmonline.client.resources.textures.ImageTextureLoader").getDeclaredMethod("loadTexture");
        cmLoadTexture.instrument(new ExprEditor(){
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("preprocessImage", methodCall.getMethodName())){
                    methodCall.replace("$_ = com.Joedobo27.WUmod.TextureResolutionDownScalingMod.preprocessedImageHook($1, 8)");
                }
            }
        });

        CtMethod cmPrepareTexture = pool.get("com.wurmonline.client.resources.textures.ResourceTextureLoader").getMethod("prepareTexture",
                "(Lcom/wurmonline/client/resources/ResourceUrl;Ljava/lang/Object;Z)V");
        cmPrepareTexture.instrument(new ExprEditor(){
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("preprocessImage", methodCall.getMethodName())) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.TextureResolutionDownScalingMod.preprocessedImageHook($1, 8)");
                }
            }
        });

        CtMethod cmReload = pool.get("com.wurmonline.client.resources.textures.ResourceTextureLoader").getMethod("reload",
                "(Lcom/wurmonline/client/resources/ResourceUrl;Lcom/wurmonline/client/resources/ResourceUrl;)V");
        cmReload.instrument(new ExprEditor(){
            @Override
            public void edit(MethodCall methodCall) throws CannotCompileException {
                if (Objects.equals("preprocessImage", methodCall.getMethodName())) {
                    methodCall.replace("$_ = com.Joedobo27.WUmod.TextureResolutionDownScalingMod.preprocessedImageHook($1, 8)");
                }
            }
        });
    preprocessedImageHook(new BufferedImage(5,5,1), 8);
    }

    @SuppressWarnings("unused")
    static PreProcessedTextureData preprocessedImageHook(BufferedImage img, int downScaleMultiple) {
        final long startTime = System.nanoTime();
        int width = img.getWidth() / downScaleMultiple;
        int height = img.getHeight() / downScaleMultiple;
        float scale = 1.0f / downScaleMultiple;
        final boolean hasAlpha = img.getColorModel().hasAlpha();
        final BufferedImage targetImage = new BufferedImage(width, height, hasAlpha ? 6 : 5);
        final Graphics2D g = (Graphics2D)targetImage.getGraphics();
        if (scale != 1.0f) {
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, Options.getTextureScalingHint());
            g.setTransform(AffineTransform.getScaleInstance(scale, scale));
        }
        if (hasAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(img, 0, 0, null);
        g.dispose();
        final byte[] data = (byte[])targetImage.getRaster().getDataElements(0, 0, width, height, null);
        return new PreProcessedTextureData(data, width, height, hasAlpha);
    }
}

