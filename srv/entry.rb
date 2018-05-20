require 'fileutils'
require 'sinatra'
require 'active_record'
require 'pry'

$sum = 0

def get_connection()
    ActiveRecord::Base.establish_connection()
    ActiveRecord::Base.connection
end

def get_parent_directory(filename)
    filename.split(/\//)[0..-2].join('/')
end

def persist_files()
    get_connection
    begin
        file_entries = []
        Dir.glob("world/**/*").each do |f|
            if File.file?(f)
                puts "[SYNC] Queuing file for persistence: #{f}"
                file_entries << {
                    file_name: f,
                    file_contents: ActiveRecord::Base.connection.escape_bytea(
                        File.open(f, 'rb').read
                    )
                }
            end
        end

        return if file_entries.empty?

        puts "[SYNC] Truncating table before backing up"
        get_connection.execute('TRUNCATE TABLE world_files;')


        puts "[SYNC] Bulk inserting blobs into postgres"
        sql = "INSERT INTO world_files (file_name, file_contents)
              VALUES #{file_entries.map { |entry| "(#{entry.values.map{|item| "'#{item}'"}.join(", ")} )" }.join(", ")}"

        get_connection.execute(sql)
    rescue => e
        puts e.message
        # binding.pry
    end
end

def restore_files()
    begin
        get_connection
            .exec_query('SELECT file_name, file_contents FROM world_files;')
            .entries
            .each do |entry|
                puts "[SYNC] Restoring file: ./#{entry['file_name']}"
                FileUtils.mkdir_p get_parent_directory("./#{entry['file_name']}")
                File.open("./#{entry['file_name']}", 'w') do |output|
                    output.print(
                        ActiveRecord::Base.connection.unescape_bytea(entry['file_contents'])
                    )
                end
            end
    rescue => e
        puts e.message
        # binding.pry
    end

end

Thread.new do # trivial example work thread
  while true do
    if Dir.exists?("world")
        puts "[SYNC] Persisting files"
        persist_files
        puts "[SYNC] Persisting complete"
    else
        puts "[SYNC] Restoring files"
        # restore_files
        puts "[SYNC] Restoring complete"
    end

    sleep 10
  end
end

get '/sum' do
  "Testing background work thread: sum is #{$sum}"
end


get '/' do
    # Landing route,
    open('ngrok.log') do |f|
        f.lines.select { |line| line.include?("URL:") }.each do |line|
            return "Server available at: #{line.match(/tcp:\/\/(.+:[0-9]+) /)[1]}"
        end
    end
end
