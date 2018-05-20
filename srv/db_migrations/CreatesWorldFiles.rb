class CreatesWorldFiles < ActiveRecord::Migration[4.2]
  def change
    create_table :world_files do |t|
      t.text :name
      t.binary :content
    end
  end
end