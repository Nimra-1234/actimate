import subprocess
import argparse
from datetime import datetime

def git_commit_with_date(commit_message, commit_date, username, email):
    """
    Makes a git commit with a custom date for existing changes.
    
    Args:
        commit_message (str): The commit message
        commit_date (str): The date and time in format 'YYYY-MM-DD HH:MM:SS'
        username (str): Git username
        email (str): Git email
    """
    try:
        # Set the Git author and committer information
        subprocess.run(['git', 'config', 'user.name', username], check=True)
        subprocess.run(['git', 'config', 'user.email', email], check=True)
        
        # Add all changes
        subprocess.run(['git', 'add', '.'], check=True)
        
        # Format the date for Git (ISO 8601 format)
        date_obj = datetime.strptime(commit_date, '%Y-%m-%d %H:%M:%S')
        git_date_format = date_obj.strftime('%Y-%m-%dT%H:%M:%S')
        
        # Make the commit with the custom date
        env = {
            'GIT_AUTHOR_DATE': git_date_format,
            'GIT_COMMITTER_DATE': git_date_format
        }
        
        # The commit command
        commit_cmd = ['git', 'commit', '-m', commit_message]
        
        # Run the commit command with the environment variables
        subprocess.run(commit_cmd, env=env, check=True)
        
        print(f"Successfully committed with message: '{commit_message}' and date: {commit_date}")
        
    except subprocess.CalledProcessError as e:
        print(f"Error during git operation: {e}")
    except ValueError as e:
        print(f"Date format error: {e}")

if __name__ == "__main__":
    # Set your constants here - replace with your actual credentials
    USERNAME = "Nimra-1234"
    EMAIL = "nimratahir1212@gmail.com"
    
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Make a git commit with a custom date.')
    parser.add_argument('--message', '-m', type=str, required=True, help='Commit message')
    parser.add_argument('--date', '-d', type=str, required=True, 
                        help='Commit date and time in format YYYY-MM-DD HH:MM:SS')
    
    args = parser.parse_args()
    
    # Execute the commit
    git_commit_with_date(args.message, args.date, USERNAME, EMAIL)