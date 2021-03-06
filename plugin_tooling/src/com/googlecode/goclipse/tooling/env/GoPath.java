/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.googlecode.goclipse.tooling.env;

import static java.util.Collections.unmodifiableList;
import static melnorme.utilbox.core.CoreUtil.listFrom;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import melnorme.utilbox.misc.MiscUtil;
import melnorme.utilbox.misc.StringUtil;

import com.googlecode.goclipse.tooling.GoPackageName;
import com.googlecode.goclipse.tooling.GoPackagesVisitor;

/**
 * Helper class to work with a GOPATH entry list.
 */
public class GoPath {
	
	public static final String SRC_DIR = "src";
	
	protected final List<String> goPathElements;
	
	public GoPath(String goPathString) {
		this(StringUtil.splitToList(goPathString, File.pathSeparator));
	}
	
	public GoPath(Collection<String> goPathElements) {
		// Use HashSet to remove duplicates
		LinkedHashSet<String> newElements = new LinkedHashSet<>();
		for (String string : goPathElements) {
			if(!string.isEmpty()) {
				newElements.add(string);
			}
		}
		this.goPathElements = unmodifiableList(new ArrayList<>(newElements));
	}
	
	public List<String> getGoPathEntries() {
		return goPathElements;
	}
	
	public String asString() {
		return getGoPathWorkspaceString(); 
	}
	
	public String getGoPathWorkspaceString() {
		return StringUtil.collToString(goPathElements, File.pathSeparator);
	}
	
	/** @return the full path of a GOPATH workspace entry (a workspace root) that contains the given path. */
	public Path findGoPathEntry(Path path) {
		if(path == null) {
			return null;
		}
		for (String pathElement : goPathElements) {
			if(path.startsWith(pathElement)) {
				return MiscUtil.createPathOrNull(pathElement);
			}
		}
		return null;
	}
	
	/** @return the GOPATH entry that contains the given goModulePath, if it's in the "src" folder of that entry. 
	 * Return null otherwise. */
	public Path findGoPathEntryForSourcePath(Path sourcePath) {
		Path workspaceEntry = findGoPathEntry(sourcePath);
		
		if(workspaceEntry != null && sourcePath.startsWith(workspaceEntry.resolve(SRC_DIR))) {
			return workspaceEntry;
		}
		return null;
	}
	
	/** @return the Go package path for given goModulePath, if it's in a source package in some GOPATH entry. 
	 * Return null otherwise. */
	public GoPackageName findGoPackageForSourceFile(Path sourceFilePath) {
		Path goPathEntry = findGoPathEntry(sourceFilePath);
		if(goPathEntry == null) {
			return null;
		}
		
		Path sourceRoot = goPathEntry.resolve(SRC_DIR);
		return GoEnvironment.getGoPackageForSourceFile(sourceFilePath, sourceRoot);
	}
	
	public static GoPackageName getGoPackageForPath(Path goPathEntry, Path dirFullPath) {
		Path sourceRoot = goPathEntry.resolve(SRC_DIR);
		if(!dirFullPath.startsWith(goPathEntry)) {
			return null;
		}
		return GoPackageName.fromPath(sourceRoot.relativize(dirFullPath));
	}
	
	public boolean isEmpty() {
		return goPathElements.size() == 0;
	}
	
	public Collection<GoPackageName> findSourcePackages(java.nio.file.Path path) {
		Path goPathEntry = findGoPathEntry(path);
		if(goPathEntry == null) {
			return new HashSet<>();
		}
		
		GoPackagesVisitor goPackagesVisitor = new GoPackagesVisitor(goPathEntry, listFrom(path)) {
			
			@Override
			protected FileVisitResult handleFileVisitException(Path file, IOException exc) {
				// TODO: some sort of logging
				return FileVisitResult.CONTINUE;
			}
		};
		
		return goPackagesVisitor.getModuleNames();
	}
	
}