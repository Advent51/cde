/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.core.writer;

/**
 * Allows passing context information during the writing process.
 * 
 * @author dcleao
 */
public interface IThingWriteContext
{
  boolean getBreakOnError();

  IThingWriterFactory getFactory();
}
