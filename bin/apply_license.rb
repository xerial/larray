
require 'tempfile'
require 'fileutils'
require 'nkf'

Dir["src/**/*.{scala,java}"].each { |file|
  lines = open(file) { |f| f.readlines }
  next if lines[1].include?("Copyright")

  out = Tempfile.new("tempfile")
  puts "apply license to #{file}"
  license = <<LICENSE
/*--------------------------------------------------------------------------
 *  Copyright 2013 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
LICENSE
  out.puts NKF.nkf('-Lw', license) 
  lines.each { |l| out.print(NKF.nkf('-Lw', l)) }
  out.close
  
  FileUtils.mv(out.path, file)
  
}




